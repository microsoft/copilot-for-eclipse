# GitHub Copilot for Eclipse
GitHub Copilot for Eclipse is a plugin that brings the power of [GitHub Copilot](https://github.com/features/copilot) to Eclipse. It provides AI-powered code completions and suggestions for Java, Python, and other languages.

## Prerequisites
- [Eclipse IDE](https://www.eclipse.org/downloads/)
- [Java 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or above
- An active [GitHub Copilot subscription](https://github.com/features/copilot)

## Getting Started
1. Find the latest release of the plugin from the [nightly build pipeline on Azure DevOps](https://mseng.visualstudio.com/VSJava/_build?definitionId=19562&_a=summary)

2. Open the latest relase pipeline and select artifacts under the `Build` job:
<p align="center">
  <img src="./docs/adoBuildArtifacts.png" alt="alt text" width="280">
</p>

3. Select `GitHubCopilotForEclipse.zip` and download it:
<p align="center">
  <img src="./docs/adoDownloadZip.png" alt="alt text" width="280">
</p>

4. Open Eclipse and go to `Help` -> `Install New Software...`:
<p align="center">
  <img src="./docs/eclipseInstallNewSoftware.png" alt="alt text" width="350">
</p>

5. Click `Add...` -> `Archive` and select the downloaded zip file:
<p align="center">
  <img src="./docs/eclipseSelectZip.png" alt="alt text" width="600">
</p>

6. Select the `GitHub Copilot` plugin and deselect `Contact all update sites during install to find required software`:
<p align="center">
  <img src="./docs/eclipseInstallNext.png" alt="alt text" width="600">
</p>

7. Click `Next` and finish the installation process:
<p align="center">
  <img src="./docs/eclipseFinish.png" alt="alt text" width="600">
</p>

8. Restart Eclipse, and the GitHub Copilot plugin is located on the bottom right corner. You are ready to use GitHub Copilot for Eclipse!
<p align="center">
  <img src="./docs/githubCopilotIconMenu.png" alt="alt text" width="280">
</p>

## Reporting Issues
Please report any issues or feedback on the [GitHub Copilot for Eclipse GitHub repository issues](https://github.com/microsoft/copilot-eclipse/issues/new?template=bug_report.md).

