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
    }

    private val repository = RepositoryService(githubClient).repositories.find {
        it.name == reactiveConfig.repositoryName
    } ?: throw IllegalArgumentException("Cannot find repository matching: ${reactiveConfig.repositoryName}")

    private val contentClient = ContentsService(githubClient)

    private val reactive = loadReactive()

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
            println(exception.localizedMessage)
            null
        }
    }

    private val labelClient = LabelService(githubClient)

    private val managedLabels = generateLabels()

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

    private val diffFiles = getDiffPaths()

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
        return reactive?.reactions ?: emptyList()
    }

    fun getPassedReactions(): List<Reaction> {
        return getReactions().filter { reaction ->
            val rules = reaction.rules
            rules.all { ruleEntry ->
                fun doesPass(statusCheck: String, regex: Regex): ((CommitFile) -> Boolean) {
                    return {
                        regex.matches(it.filename) && it.status == statusCheck
                    }
                }

                val addedChecks = ruleEntry.value.added
                    .map { Regex(it) }
                    .map { doesPass("added", it) }
                val modifiedChecks = ruleEntry.value.modified
                    .map { Regex(it) }
                    .map { doesPass("modified", it) }
                when (ruleEntry.key) {
                    RuleResult.Truths -> addedChecks.all {
                        diffFiles.any(it)
                    } && modifiedChecks.all {
                        diffFiles.any(it)
                    }
                    RuleResult.Lies -> addedChecks.all {
                        diffFiles.none(it)
                    } && modifiedChecks.all {
                        diffFiles.none(it)
                    }
                }
            }
        }
    }

    private val issueClient = IssueService(githubClient)

    fun applyLabels(labels: List<String>) {
        val toAddLabels = managedLabels.filter {
            labels.any { labelName ->
                it.name == labelName
            }
        }
        val issue = issueClient.getIssue(repository, reactiveConfig.issueNumber)
        val newLabels = issue.labels.filterNot { label ->
            managedLabels.any {
                label.name == it.name
            }
        }.toMutableList().apply {
            addAll(toAddLabels)
        }.toList()
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