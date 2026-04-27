<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# datamancer Changelog

## [0.0.10]
### Added
- Configurable excluded directories for dbt projects (default: `target`, `logs`, `dbt_packages`)
  - Excluded directories are marked via IntelliJ's content entry exclusion, removing them from IDE indexing, search, and file traversal
  - Per-project setting editable in Settings > Tools > dbt Projects
  - `dbt_project.yml` files inside excluded directories (e.g. packages) are no longer discovered as separate projects
  - Defence-in-depth filtering in source and column metadata index services
