# Contributing to OpenRS2

## Introduction

OpenRS2 is still in the early stages of development. The current focus is on
building underlying infrastructure, such as the deobfuscator, rather than
game content. This approach will make it much quicker to build game content in
the long run, but it does mean OpenRS2 won't be particularly useful in the short
term.

If you're interested in contributing new features, you should discuss your
plans in our [Discord][discord] server first. I have rough plans in my head for
the future development direction. Communicating beforehand will avoid the need
for significant changes to be made at the code review stage and make it less
likely for your contribution to be dropped entirely.

## Code style

All source code must be formatted with [IntelliJ IDEA][idea]'s built-in
formatter before each commit. The 'Optimize imports' option should also be
selected. Do not select 'Rearrange entries'.

OpenRS2's code style settings are held in `.idea/codeStyles/Project.xml` in the
repository, and IDEA should use them automatically after importing the Gradle
project.

Kotlin code must pass all of [ktlint][ktlint]'s tests.

Always use `//` for single-line comments and `/*` for multi-line comments.

## Commit messages

Commit messages should follow the ['seven rules'][commitmsg] described in
'How to Write a Git Commit Message', with the exception that the summary line
can be up to 72 characters in length (as OpenRS2 does not use email-based
patches).

You should use tools like [interactive rebase][rewriting-history] to ensure the
commit history is tidy.

## Developer Certificate of Origin

OpenRS2 uses version 1.1 of the [Developer Certificate of Origin][dco] (DCO) to
certify that contributors agree to license their code under OpenRS2's license
(see the License section below). To confirm that a contribution meets the
requirements of the DCO, a `Signed-off-by:` line must be added to the Git
commit message by passing `--signoff` to the `git commit` invocation.

If you intend to make a large number of contributions, run the following
commands from the repository root to add `Signed-off-by:` line to all your
commit messages by default:

```
echo -e "\n\nSigned-off-by: $(git config user.name) <$(git config user.email)>" > .git/commit-template
git config commit.template .git/commit-template
```

The full text of the DCO is available in the `DCO` file.

OpenRS2 does not distribute any of Jagex's intellectual property, and care
should be taken to avoid inadvertently including any in contributions.

## Versioning

OpenRS2 uses [Semantic Versioning][semver].

## Gitea

OpenRS2 only uses GitHub as a mirror. Issues and pull requests should be
submitted to [OpenRS2's self-hosted Gitea instance][gitea].

[commitmsg]: https://chris.beams.io/posts/git-commit/#seven-rules
[dco]: https://developercertificate.org/
[discord]: https://chat.openrs2.org/
[gitea]: https://git.openrs2.org/openrs2/openrs2
[idea]: https://www.jetbrains.com/idea/
[ktlint]: https://github.com/pinterest/ktlint#readme
[rewriting-history]: https://git-scm.com/book/en/v2/Git-Tools-Rewriting-History
[semver]: https://semver.org/
