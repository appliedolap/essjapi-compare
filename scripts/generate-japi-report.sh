#!/usr/bin/env bash
#
# Generate the Essbase Java API Evolution report from one or more folders of
# versioned Essbase client JARs.
#
# Each SOURCE_DIR is expected to hold one subfolder per Essbase version, named
# with the version number, containing the JAPI jar. Collections in the wild use
# different filenames for that jar, so both of these layouts work:
#
#   <SOURCE_DIR>/11.1.2.4.048/ess_japi.jar          (connector version-files)
#   <SOURCE_DIR>/7.1.3/ess_japi-7.1.3.jar           (essbase-jar-deployer staging)
#
# Multiple SOURCE_DIRs are merged into a single staging tree, so collections
# covering different version ranges can be combined into one report. The staging
# tree is built from symlinks named canonically, so nothing is copied, the source
# folders are untouched, and mixed naming conventions are normalized.
#
# The Essbase jars are Oracle-licensed and must never be committed to this repo.
# Only the generated HTML is intended for publishing.

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

CANONICAL_JAR="ess_japi.jar"
JAR_PATTERN="ess_japi*.jar"
JAR_EXACT=""
OUTPUT_FILE="$PROJECT_ROOT/build/essbase-java-api-evolution/index.html"
STAGING_DIR=""
KEEP_STAGING=0
DRY_RUN=0
ONLY=""

usage() {
	cat <<'USAGE'
Usage: scripts/generate-japi-report.sh [options] SOURCE_DIR [SOURCE_DIR...]

Options:
  -o, --output FILE      Output HTML file
                         (default: build/essbase-java-api-evolution/index.html)
  -p, --jar-pattern GLOB Glob for locating the JAPI jar inside each version
                         folder (default: ess_japi*.jar)
  -j, --jar-name NAME    Require this exact jar filename instead of globbing
  -s, --staging DIR      Staging directory to build (default: a temp dir)
  -k, --keep-staging     Do not delete the staging directory on exit
  -n, --dry-run          Stage and report coverage, but do not generate
      --only LIST        Comma-separated version numbers to include, ignoring the rest.
                         Comparing 117 jars takes seconds; when the change being made is to
                         the template rather than the analysis, a handful is enough and the
                         loop gets far tighter.
  -h, --help             Show this help

Sources are merged in the order given; if two sources supply the same version,
the first one wins.

Examples:
  # Full history: the deployer staging collection plus the newer connector jars
  scripts/generate-japi-report.sh \
    "../Dodeca/Java/Tools/essbase-jar-deployer/staging" \
    "../Dodeca/Connectors/dodeca-essbase-connector/version-files" \
    "../Outline Extractor NG/version-files"

  # See what coverage you'd get without doing the work
  scripts/generate-japi-report.sh --dry-run /path/to/jars

  # A quick subset while iterating on the template
  scripts/generate-japi-report.sh --only 9.3.0,9.3.3,11.1.2,21.8.2.0.0.031 /path/to/jars
USAGE
}

SOURCES=()
while [ $# -gt 0 ]; do
	case "$1" in
		-o|--output)       OUTPUT_FILE="$2"; shift 2 ;;
		-p|--jar-pattern)  JAR_PATTERN="$2"; shift 2 ;;
		-j|--jar-name)     JAR_EXACT="$2"; shift 2 ;;
		-s|--staging)      STAGING_DIR="$2"; shift 2 ;;
		-k|--keep-staging) KEEP_STAGING=1; shift ;;
		-n|--dry-run)      DRY_RUN=1; shift ;;
		--only)            ONLY=",$2,"; shift 2 ;;
		-h|--help)         usage; exit 0 ;;
		-*)                echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
		*)                 SOURCES+=("$1"); shift ;;
	esac
done

if [ ${#SOURCES[@]} -eq 0 ]; then
	echo "Error: at least one SOURCE_DIR is required." >&2
	usage >&2
	exit 2
fi

for src in "${SOURCES[@]}"; do
	if [ ! -d "$src" ]; then
		echo "Error: source directory not found: $src" >&2
		exit 1
	fi
done

# ---------------------------------------------------------------------------
# Locate the JAPI jar within a version folder
#
# Collections name this jar inconsistently, so prefer an exact match on the
# canonical name and fall back to the glob. Ambiguity is reported rather than
# guessed at, since silently picking the wrong jar would corrupt the report.
# ---------------------------------------------------------------------------

resolve_jar() {
	local dir="$1"

	if [ -n "$JAR_EXACT" ]; then
		if [ -f "$dir/$JAR_EXACT" ]; then
			printf '%s' "$dir/$JAR_EXACT"
			return 0
		fi
		return 1
	fi

	if [ -f "$dir/$CANONICAL_JAR" ]; then
		printf '%s' "$dir/$CANONICAL_JAR"
		return 0
	fi

	local matches=()
	while IFS= read -r line; do
		[ -n "$line" ] && matches+=("$line")
	done < <(find "$dir" -maxdepth 1 -type f -name "$JAR_PATTERN" 2>/dev/null | sort)

	case ${#matches[@]} in
		0) return 1 ;;
		1) printf '%s' "${matches[0]}"; return 0 ;;
		*) return 2 ;;
	esac
}

# ---------------------------------------------------------------------------
# Staging tree
# ---------------------------------------------------------------------------

if [ -z "$STAGING_DIR" ]; then
	STAGING_DIR="$(mktemp -d "${TMPDIR:-/tmp}/essjapi-stage.XXXXXX")"
	cleanup() {
		if [ "$KEEP_STAGING" -ne 1 ]; then
			rm -rf "$STAGING_DIR"
		fi
	}
	trap cleanup EXIT
else
	mkdir -p "$STAGING_DIR"
fi

echo "Staging: $STAGING_DIR"

staged=0
skipped_no_jar=0
skipped_not_version=0
skipped_ambiguous=0
duplicates=0

for src in "${SOURCES[@]}"; do
	src="$(cd "$src" && pwd)"
	echo "Scanning: $src"
	found_here=0

	for dir in "$src"/*/; do
		[ -d "$dir" ] || continue
		dir="${dir%/}"
		version="$(basename "$dir")"

		# The Version parser only accepts numeric components separated by dots
		# or underscores; anything else would abort the whole run.
		if ! [[ "$version" =~ ^[0-9]+([._][0-9]+)*$ ]]; then
			skipped_not_version=$((skipped_not_version + 1))
			continue
		fi

		if [ -n "$ONLY" ] && [ "${ONLY#*,"$version",}" = "$ONLY" ]; then
			continue
		fi

		if [ -e "$STAGING_DIR/$version/$CANONICAL_JAR" ]; then
			duplicates=$((duplicates + 1))
			continue
		fi

		set +e
		jar_path="$(resolve_jar "$dir")"
		rc=$?
		set -e

		case $rc in
			1)
				echo "  skip (no jar matching '${JAR_EXACT:-$JAR_PATTERN}'): $version"
				skipped_no_jar=$((skipped_no_jar + 1))
				continue
				;;
			2)
				echo "  skip (multiple jars match '$JAR_PATTERN', use --jar-name): $version"
				skipped_ambiguous=$((skipped_ambiguous + 1))
				continue
				;;
		esac

		mkdir -p "$STAGING_DIR/$version"
		# Normalize the filename so the tool sees one consistent jar name
		ln -s "$jar_path" "$STAGING_DIR/$version/$CANONICAL_JAR"
		staged=$((staged + 1))
		found_here=$((found_here + 1))
	done

	echo "  contributed $found_here version(s)"
done

echo
echo "Staged $staged version(s)."
if [ "$skipped_no_jar" -gt 0 ]; then
	echo "  $skipped_no_jar skipped: no matching jar"
fi
if [ "$skipped_ambiguous" -gt 0 ]; then
	echo "  $skipped_ambiguous skipped: ambiguous jar name"
fi
if [ "$skipped_not_version" -gt 0 ]; then
	echo "  $skipped_not_version skipped: folder name is not a version number"
fi
if [ "$duplicates" -gt 0 ]; then
	echo "  $duplicates skipped: version already supplied by an earlier source"
fi

if [ "$staged" -lt 2 ]; then
	echo "Error: need at least 2 versions to produce a comparison (staged $staged)." >&2
	exit 1
fi

if [ "$DRY_RUN" -eq 1 ]; then
	echo
	echo "Dry run; versions that would be compared:"
	ls -1 "$STAGING_DIR" | sort -t. -k1,1n -k2,2n -k3,3n -k4,4n -k5,5n -k6,6n | tr '\n' ' '
	echo
	exit 0
fi

# ---------------------------------------------------------------------------
# Build and run
# ---------------------------------------------------------------------------

CLASSPATH_FILE="$PROJECT_ROOT/target/japi-classpath.txt"

echo
echo "Compiling..."
mvn -q -B -f "$PROJECT_ROOT/pom.xml" compile

if [ ! -f "$CLASSPATH_FILE" ] || [ "$PROJECT_ROOT/pom.xml" -nt "$CLASSPATH_FILE" ]; then
	echo "Resolving dependency classpath..."
	mvn -q -B -f "$PROJECT_ROOT/pom.xml" dependency:build-classpath \
		-Dmdep.outputFile="$CLASSPATH_FILE"
fi

mkdir -p "$(dirname "$OUTPUT_FILE")"

echo "Generating report ($staged versions, $((staged - 1)) comparisons)..."
echo

java -cp "$PROJECT_ROOT/target/classes:$(cat "$CLASSPATH_FILE")" \
	com.appliedolap.essjapicompare.JapiCompare \
	--base-folder "$STAGING_DIR" \
	--version-folders \
	--jar-name "$CANONICAL_JAR" \
	--output-file "$OUTPUT_FILE"

echo
echo "Wrote: $OUTPUT_FILE"
if [ -f "$OUTPUT_FILE" ]; then
	echo "Size:  $(du -h "$OUTPUT_FILE" | cut -f1)"
fi
