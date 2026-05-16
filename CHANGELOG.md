<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# datamancer Changelog

## [0.0.11]
### Added
- Gutter icon on dbt SQL model files for navigating to the corresponding YAML schema definition (reverse of the existing YAML->SQL go-to-definition)
- "Go to YAML Definition" action in the Navigate menu for the same navigation via keyboard

## [0.0.10]
### Added
- Configurable excluded directories for dbt projects (default: `target`, `logs`, `dbt_packages`)
  - Excluded directories are marked via IntelliJ's content entry exclusion, removing them from IDE indexing, search, and file traversal
  - Per-project setting editable in Settings > Tools > dbt Projects
  - `dbt_project.yml` files inside excluded directories (e.g. packages) are no longer discovered as separate projects
  - Defence-in-depth filtering in source and column metadata index services
