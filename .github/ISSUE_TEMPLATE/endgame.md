---
name: Endgame
about: Template for release endgame list
title: "[Endgame] "
labels: ''
assignees: ''

---

### Download bit

tbd

### Issues to verify

- [ ] 

### Release steps
- [ ] Trigger Nightly build: https://mseng.visualstudio.com/VSJava/_build?definitionId=19562
- [ ] Trigger RC build: https://mseng.visualstudio.com/VSJava/_build?definitionId=19982
- [ ] Verify issues
- [ ] Fix blocking issues for release
- [ ] Update test plan
  - [ ] @jdneo 
  - [ ] @duzitong 
  - [ ] @ethanyhou 
  - [ ] @xinyi-gong
- [ ] Create a new branch from `main`, named as `release/<version>`
- [ ] Release Nightly: https://mseng.visualstudio.com/VSJava/_build?definitionId=20768
- [ ] Update solution version for nightly on marketplace: https://marketplace.eclipse.org/content/github-copilot-nightly/edit
- [ ] Release RC: https://mseng.visualstudio.com/VSJava/_build?definitionId=21364
- [ ] Update solution version for stable on marketplace: https://marketplace.eclipse.org/content/github-copilot/edit
- [ ] Add release in https://github.com/microsoft/copilot-for-eclipse/releases, makes sure target branch is `release/<version>` for the tag
