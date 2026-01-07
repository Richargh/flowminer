# Teamcharta Project Commands
# Run `just --list` to see all available recipes

# Default recipe - show available commands
default:
    @just --list

# =============================================================================
# Lint COMMANDS
# =============================================================================
lint: lint-viz

lint-viz: lint-viz-types

lint-viz-types:
    cd visualization && npx tsc --noEmit


# =============================================================================
# BUILD COMMANDS
# =============================================================================

# Build entire project (Gradle + visualization)
build: build-analysis build-kmp-js build-viz

# Build all Gradle projects (compile only, no tests, includes KMP-JS dist)
build-analysis:
    ./gradlew assemble

# Build KMP JS distributions for browser
build-kmp-js:
    ./gradlew :shared:jsBrowserProductionLibraryDistribution
    ./gradlew :analysis:git-importer:jsBrowserProductionLibraryDistribution

# Build visualization (TypeScript + Vite)
build-viz: lint-viz
    cd visualization && npm run build


# =============================================================================
# DEVELOPMENT COMMANDS
# =============================================================================

# Start visualization dev server
dev-viz:
    cd visualization && npm run dev

# Preview visualization production build
preview-viz:
    cd visualization && npm run preview

# Install visualization dependencies
install-viz:
    cd visualization && npm install

# =============================================================================
# CLEAN COMMANDS
# =============================================================================

# Clean all build artifacts
clean:
    ./gradlew clean
    rm -rf visualization/dist


# =============================================================================
# TEST COMMANDS
# =============================================================================

# Run all tests (Gradle + visualization)
test: test-analysis test-viz

# Run all Gradle tests (KMP via allTests + non-KMP via test)
test-analysis:
    ./gradlew allTests test

# Run Gradle JVM tests only (KMP jvmTest + non-KMP test)
test-analysis-jvm:
    ./gradlew jvmTest test

# Run Gradle JS tests only
test-analysis-js:
    ./gradlew jsTest

# Run visualization tests
test-viz:
    cd visualization && npm test

# Run tests for a specific Gradle subproject (KMP: allTests, non-KMP: test)
test-analysis-project project:
    ./gradlew :{{project}}:allTests --dry-run 2>/dev/null && ./gradlew :{{project}}:allTests || ./gradlew :{{project}}:test

# Run JVM tests for a specific Gradle subproject (KMP: jvmTest, non-KMP: test)
jvm-test-analysis-project project:
    ./gradlew :{{project}}:jvmTest --dry-run 2>/dev/null && ./gradlew :{{project}}:jvmTest || ./gradlew :{{project}}:test

# Run JS tests for a specific Gradle subproject
js-test-analysis-project project:
    ./gradlew :{{project}}:jsTest

# =============================================================================
# RUN ANALYSIS COMMANDS
# =============================================================================

# Run gitcli
# f.ex. just run-gitcli '. --format table --since "'2 weeks ago'"'
run-gitcli *args:
    ./gradlew :analysis:gitcli:run --args='{{args}}'

# Run fmsh (Flow-Miner Shell)
# f.ex. just fmsh 'git-commits . --format table'
fmsh *args:
    ./gradlew :analysis:fmsh:run --args='{{args}}' --quiet

# =============================================================================
# DISTRIBUTION COMMANDS
# =============================================================================

# Build fmsh distribution (tar)
dist-fmsh:
    ./gradlew :analysis:fmsh:distTar
    @echo "Built: analysis/fmsh/build/distributions/fmsh-*.tar"

# Build visualization distribution (tar.gz)
dist-viz: lint-viz
    cd visualization && npm run dist
    @echo "Built: visualization/visualization-*.tar.gz"

# =============================================================================
# CI/CD HELPERS
# =============================================================================

# Run Gradle tests with CI optimizations (parallel, no daemon)
test-analysis-ci:
    ./gradlew allTests test --parallel --no-daemon --continue
