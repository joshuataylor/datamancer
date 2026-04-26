# Contributing to Datamancer

Developer guide for the Datamancer IntelliJ Platform plugin.

## Prerequisites

- IntelliJ IDEA Ultimate or PyCharm Professional (for development)
- JDK 21 and Kotlin 2.3.21 (both handled automatically by the Gradle toolchain)
- Do not run `./gradlew` from the terminal. Use the IDE's Gradle integration, or ask the project owner to build.

## Development Setup

1. Clone the repository
2. Open the root directory in IntelliJ IDEA as a Gradle project
3. The `build-logic/` convention plugin configures Kotlin, IntelliJ Platform, and all dependencies automatically
4. To run a sandboxed IDE: execute the `:datamancer-dbt-core:runIde` Gradle task. This launches a PyCharm instance with both the core and dbt-core plugins installed.

The `localPlugin(project(":datamancer-core"))` dependency in backend `build.gradle.kts` files ensures core is compiled and installed in the sandbox automatically.

## Plugin Lifecycle

When a project opens, the plugin initialises in this order:

1. `DatamancerProjectStartupActivity` (`postStartupActivity`) -- waits for smart mode (file indexing complete)
2. `DatamancerProjectDiscoveryService.discoverAndAssociate()` -- scans for `dbt_project.yml` files via `FilenameIndex`, maps each to its containing module, writes `DatamancerProjectConfig` to the workspace model external mapping
3. `DatamancerDbtProjectIndexService.start()` -- reads all external mappings into a `ConcurrentHashMap` cache, subscribes to workspace model event log for ongoing changes
4. Language features activate -- `DatamancerLanguageSubstitutor` starts returning `Jinja2Language` for `.sql` files in dbt modules, which enables Jinja2 parsing, references, and completions

After this, reference resolution (`ref()`, `source()`, `var()`), SQL column completion, and YAML navigation all function.

## Common Tasks

### Adding a new Jinja2 reference provider

Example: supporting a new function like `metric('name')`.

1. Create `DbtMetricRefReferenceProvider.kt` in `datamancer-core/.../core/lang/`:
    - Define a `PatternCondition<Jinja2FunctionCall>` that checks `callee.name == "metric"`
    - Define a `REF_FUNCTION_PATTERN` using `PlatformPatterns.psiElement(Jinja2FunctionCall::class.java).with(condition)`
    - Implement `getReferencesByElement()` to extract the string argument and return a reference
    - Pattern to follow: `DbtModelRefReferenceProvider` (`datamancer-core/.../core/lang/DbtModelRefReferenceProvider.kt`)

2. Create `DbtMetricRefReference.kt` extending `DbtModelReferenceBase<Jinja2FunctionCall>`:
    - Implement `resolve()` to find the target PSI element
    - Implement `getVariants()` for code completion
    - Pattern to follow: `DbtModelRefReference` (`datamancer-core/.../core/lang/DbtModelRefReference.kt`)

3. Register in `DbtReferenceContributor.registerReferenceProviders()`:
   ```kotlin
   registrar.registerReferenceProvider(
       DbtMetricRefReferenceProvider.METRIC_FUNCTION_PATTERN,
       DbtMetricRefReferenceProvider()
   )
   ```

The contributor is already registered for both `Jinja2` and `DjangoTemplate` languages in `plugin.xml`, so no XML changes are needed.

### Adding a new completion contributor

Pattern to follow: `DbtConfigCompletionContributor` (`datamancer-core/.../core/lang/DbtConfigCompletionContributor.kt`).

1. Create your contributor class extending `CompletionContributor`
2. In `init`, call `extend(CompletionType.BASIC, pattern, provider)`
3. Register in `datamancer-core/src/main/resources/META-INF/plugin.xml`:
   ```xml
   <completion.contributor language="Jinja2"
       implementationClass="com.github.joshuataylor.datamancer.core.lang.YourCompletionContributor"/>
   <completion.contributor language="DjangoTemplate"
       implementationClass="com.github.joshuataylor.datamancer.core.lang.YourCompletionContributor"/>
   ```
   Register for both languages to cover all Jinja2 template contexts.

### Adding a new Jinja2 tag or function to code completion

Edit `DbtTagLibrary` (`datamancer-core/.../core/lang/DbtTagLibrary.kt`):

- For functions that accept arguments (e.g. `env_var('KEY')`): add to `DBT_PARAMETERIZED_TAGS`
- For properties/variables (e.g. `target`): add to `DBT_UNPARAMETERIZED_TAGS`

No registration changes needed -- the tag library is already registered via `templateLanguageCoreTags` in `plugin.xml`.

### Adding a new index service

Pattern to follow: `DbtSourceIndexService` (`datamancer-core/.../core/services/DbtSourceIndexService.kt`).

1. Create your service class annotated with `@Service(Service.Level.PROJECT)`
2. Use `DatamancerDbtProjectIndexService.getInstance(project).getAllDbtConfigsSync()` to scope file scanning to dbt project directories
3. Register in `plugin.xml`:
   ```xml
   <projectService
       serviceImplementation="com.github.joshuataylor.datamancer.core.services.YourIndexService"/>
   ```

### Adding a new backend

In brief:

1. Create a new subproject (e.g. `datamancer-my-backend/`)
2. Apply `datamancer-conventions` and add `localPlugin(project(":datamancer-core"))` in dependencies
3. Implement `DatamancerDbtCompilerProvider` and `DatamancerDbtExecutorProvider` from `core.api`
4. Create a `plugin.xml` registering implementations under `defaultExtensionNs="com.github.joshuataylor.datamancer.core"`
5. Add the subproject to `settings.gradle.kts`

## Testing

- Test framework: IntelliJ Platform test framework (`BasePlatformTestCase`)
- Core tests: `datamancer-core/src/test/kotlin/`
- Test dbt project: `testing/jaffle_shop_duckdb/`
- Trace logging: Enable with `idea.log.trace.categories=#com.github.joshuataylor.datamancer`
- Naming convention: New test files should use `Datamancer*Test` prefix (not the legacy `DbtHelper*Test`)

## Code Conventions

- Australian English spelling (e.g. "initialise", "colour", "materialised")
- No emojis in code or documentation
- File naming: `DatamancerSqlExtension.kt` (not `DbtSqlExtension.kt`). Never use `DbtHelper*` prefix for new files.
- IntelliJ naming: `*Impl`, `*Base`, `*Stub` suffixes where appropriate
- PSI access: Use `readAction {}` for reads, `writeAction {}` for modifications
- Coroutines: Public service APIs should be `suspend` functions (IntelliJ 2025.2+)
- Long operations: Call `ProgressManager.checkCanceled()` periodically

