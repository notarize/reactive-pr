package com.notarize.reactive

import com.notarize.reactive.model.Reaction
import com.notarize.reactive.model.Reactive
import com.notarize.reactive.model.RuleResult
import org.eclipse.egit.github.core.CommitFile
import org.eclipse.egit.github.core.Label
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.*
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.util.*

class ReactiveService(
    private val reactiveConfig: ReactiveConfig
) {

    private val githubClient = GitHubClient().apply {
        setOAuth2Token(reactiveConfig.githubAuthToken)
    }.also {
        println("Github API V3 Remaining Requests: ${it.remainingRequests} of ${it.requestLimit}")
    }

    private val repository = RepositoryService(githubClient).repositories.find {
        it.name == reactiveConfig.repositoryName
    } ?: throw IllegalArgumentException("Cannot find repository matching: ${reactiveConfig.repositoryName}")

    private val contentClient = ContentsService(githubClient)

    val reactive: Lazy<Reactive?> = lazy {
        loadReactive()
    }

    private fun loadReactive(): Reactive? {
        return try {
            contentClient.getContents(repository, "reactive.yml", reactiveConfig.baseSha).firstOrNull()?.content?.let {
                String(Base64.getMimeDecoder().decode(it))
            }?.let {
                println("Reactive YAML:\n$it")
                val yaml = Yaml()
                Reactive(yaml.load(it))
            }
        } catch (exception: IOException) {
            println("Failed to load Reactive YAML: ${exception.localizedMessage}")
            null
        }
    }

    private val labelClient = LabelService(githubClient)

    private val managedLabels: Lazy<List<Label>> = lazy {
        generateLabels()
    }

    private fun generateLabels(): List<Label> {
        val expectedLabels = getReactions().map {
            it.actionProcess.label
        }.flatten().toSet()
        println("Expected Labels: $expectedLabels")
        val existing = labelClient.getLabels(repository)
        println("Existing Labels: ${existing.map {
            it.name
        }}")
        val newLabels = expectedLabels.filterNot { expected ->
            existing.any {
                it.name == expected
            }
        }
        newLabels.map {
            Label().setName(it).setColor(generateRandomColor())
        }.forEach {
            labelClient.createLabel(repository, it)
        }
        println("Created Labels: $newLabels")
        return labelClient.getLabels(repository).filter {
            expectedLabels.any { expected ->
                it.name == expected
            }
        }
    }

    private fun generateRandomColor(): String {
        val random = Random()
        return String.format("%06x", random.nextInt(256 * 256 * 256))
    }

    private val commitClient = CommitService(githubClient)

    private val diffFiles: Lazy<List<CommitFile>> = lazy {
        getDiffPaths()
    }

    private fun getDiffPaths(): List<CommitFile> {
        return try {
            commitClient.compare(repository, reactiveConfig.baseSha, reactiveConfig.headSha).files
                .also {
                    println("Diff Paths: ${it.map { i -> "${i.filename} - ${i.status}" }}")
                }
        } catch (exception: IOException) {
            println(exception.localizedMessage)
            emptyList()
        }
    }

    private fun getReactions(): List<Reaction> {
        return reactive.value?.reactions ?: emptyList()
    }

    fun getPassedReactions(): List<Reaction> {
        return getReactions().filter { reaction ->
            val rules = reaction.rules
            rules.all { ruleEntry ->
                fun doesPass(statusCheck: String, regex: Regex): ((CommitFile) -> Boolean) {
                    return {
                        regex.containsMatchIn(it.filename) && it.status == statusCheck
                    }
                }

                val addedChecks = ruleEntry.value.added
                    .map { Regex(it) }
                    .map { doesPass("added", it) }
                val modifiedChecks = ruleEntry.value.modified
                    .map { Regex(it) }
                    .map { doesPass("modified", it) }
                val anyChecks = ruleEntry.value.any
                    .map { Regex(it) }
                    .map { regex ->
                        { it: CommitFile ->
                            doesPass("added", regex).invoke(it) || doesPass("modified", regex).invoke(it)
                        }
                    }
                when (ruleEntry.key) {
                    RuleResult.Truths -> addedChecks.all {
                        diffFiles.value.any(it)
                    } && modifiedChecks.all {
                        diffFiles.value.any(it)
                    } && anyChecks.all {
                        diffFiles.value.any(it)
                    }
                    RuleResult.Lies -> addedChecks.all {
                        diffFiles.value.none(it)
                    } && modifiedChecks.all {
                        diffFiles.value.none(it)
                    } && anyChecks.all {
                        diffFiles.value.none(it)
                    }
                }
            }
        }
    }

    private val issueClient = IssueService(githubClient)

    fun isIssueOpen(): Boolean {
        val issueState = getIssueState()
        return issueState != "closed"
    }

    fun getIssueState(): String = issueClient.getIssue(repository, reactiveConfig.issueNumber).state

    fun applyLabels(labels: List<String>) {
        val toAddLabels = managedLabels.value.filter {
            labels.any { labelName ->
                it.name == labelName
            }
        }
        val issue = issueClient.getIssue(repository, reactiveConfig.issueNumber)
        val newLabels = issue.labels.filterNot { label ->
            managedLabels.value.any {
                label.name == it.name
            }
        }.toMutableList().apply {
            addAll(toAddLabels)
        }.toList()
        if (issue.labels.containsAll(newLabels) && newLabels.containsAll(issue.labels)) return
        issue.labels = newLabels
        issueClient.editIssue(repository, issue)
    }

    fun assignReviewers(reviewers: List<String>) {
        val params = mapOf(
            "reviewers" to reviewers
        )
        val uri =
            "/${repository.url.replace(
                "https://api.github.com/",
                ""
            )}/pulls/${reactiveConfig.issueNumber}/requested_reviewers"
        githubClient.post<Any>(
            uri,
            params,
            Map::class.java
        )
    }

    fun assignGroupReviewers(reviewers: List<String>) {
        val params = mapOf(
            "team_reviewers" to reviewers
        )
        val uri =
            "/${repository.url.replace(
                "https://api.github.com/",
                ""
            )}/pulls/${reactiveConfig.issueNumber}/requested_reviewers"
        githubClient.post<Any>(
            uri,
            params,
            null
        )
    }
}
