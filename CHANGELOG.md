<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# datamancer Changelog

## [0.0.12]
### Bug Fixes
* `DatamancerDebugAction` no longer blocks the EDT -- debug info collection (which spawns Python processes) now runs in a background coroutine via `AnActionEvent.coroutineScope`, with notifications dispatched back on `Dispatchers.EDT`. - [@joshuataylor](https://github.com/joshuataylor)
* `DatamancerRunConfiguration` no longer creates an orphaned `CoroutineScope` -- compile-and-run coroutines now launch on the `DatamancerCompileService` project service scope, ensuring proper cancellation on project close. - [@joshuataylor](https://github.com/joshuataylor)

### Build
* IntelliJ Platform Gradle Plugin 2.15.0 -> 2.16.0. - [@joshuataylor](https://github.com/joshuataylor)
* Qodana plugin 2025.3.2 -> 2026.1.0. - [@joshuataylor](https://github.com/joshuataylor)

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
