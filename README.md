[![Build](https://github.com/julbme/gh-action-manage-tag/actions/workflows/maven-build.yml/badge.svg)](https://github.com/julbme/gh-action-manage-tag/actions/workflows/maven-build.yml)
[![Lint Commit Messages](https://github.com/julbme/gh-action-manage-tag/actions/workflows/commitlint.yml/badge.svg)](https://github.com/julbme/gh-action-manage-tag/actions/workflows/commitlint.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=julbme_gh-action-manage-tag&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=julbme_gh-action-manage-tag)
![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/julbme/gh-action-manage-tag)

# GitHub Action to manage tags

The GitHub Action for managing tags of the GitHub repository.

- Create a new tag
- Move the tag to another commit
- Delete a tag

## Usage

### Example Workflow file

- Create a tag:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Create/Update the tag
        uses: julbme/gh-action-manage-tag@v1
        with:
          name: tag-name
          state: present
          from: ${{ github.ref }}
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- Delete a tag

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Delete the tag
        uses: julbme/gh-action-manage-tag@v1
        with:
          name: tag-name
          state: absent
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Inputs

|  Name   |  Type  |   Default    |                                                                              Description                                                                               |
|---------|--------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `name`  | string | `Not set`    | Name of the tag. **Required**                                                                                                                                          |
| `state` | string | `present`    | Expected state of the tag. Valid values are `present` to create the tag or `absent` to delete the tag                                                                  |
| `from`  | string | `github.sha` | The reference from which to create or update the tag - could be a branch, a tag, a ref or a specific SHA. By default, it takes the commit that triggered the workflow. |

### Outputs

|  Name  |  Type  |                               Description                               |
|--------|--------|-------------------------------------------------------------------------|
| `ref`  | string | Git ref of the tag `refs/tags/name`, or ` ` in case the tag is deleted. |
| `name` | string | Name of the tag, or ` ` in case the tag is deleted.                     |
| `sha`  | sha    | SHA Commit of the tag, or ` ` in case the tag is deleted.               |

## Contributing

This project is totally open source and contributors are welcome.
