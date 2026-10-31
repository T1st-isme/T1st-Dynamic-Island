# Phase 3: Vietnamese language layer

This document covers the integrated localization layer. Onboarding, the
`AppLanguage.VI` enum addition, setup/settings fields and notification privacy
are described in [the phase 3 guide](phase-3-usability.md). Do not use the counts
below as a claim that external content or every parser supports Vietnamese.

## Language and translation contract

- `tr(ru: String, en: String, vi: String? = null)` preserves existing two-argument
  source calls. RU always returns `ru`; EN always returns `en`. For VI, explicit
  `vi` wins, otherwise an **exact, case-sensitive English key** is looked up in
  `VietnameseStrings.byEnglish`, otherwise English is returned unchanged.
- The static catalog keeps the existing inline RU/EN source of truth. This avoids
  migrating hundreds of strings and parser code to Android resources in this
  phase. There is no fuzzy matching, regex translation or template substitution.
  Editing an English label requires updating its catalog key.
- Use a third argument for interpolation, for example:
  `tr("$count мин", "$count min", "$count phút")`. The catalog cannot and should
  not guess how to translate an already interpolated string.
- `Lang.code` is `ru`, `en` or `vi`. `Lang.en` remains a compatibility flag:
  it is true for **both English and Vietnamese**. New UI language decisions must
  use `Lang.code`, not `Lang.en`.
- `SYSTEM` selects Vietnamese for a Vietnamese device locale, Russian for
  Russian, English for everything else. Explicit RU/EN/VI override the system.
  `Language.kt` contains pure, JVM-testable resolution and translation functions.
  It accepts the saved enum **name**, not an ordinal; no saved names are changed.
- `LangScope` observes settings and Android configuration, and its key includes
  the resolved language code. A SYSTEM change EN → VI therefore recreates the
  subtree even though `Lang.en` remains true.

## Formatting and Android resources

- `Lang.locale` supplies `vi-VN` for Vietnamese. The idle island's date uses
  Vietnamese weekday/month names and day-before-month order.
- Alarm and idle clock formatting use the Vietnamese locale while preserving
  Android's 12/24-hour preference. RU/EN continue using their previous clock path.
- `Lang.format` makes decimal UI values follow Vietnamese conventions even on
  an English phone: event duration, outline width and update file size use it.
  RU/EN preserve the prior system-locale number-format behavior.
- `res/values-vi/strings.xml` supplies all four existing Android resource names:
  app label, accessibility label/description and quick-settings tile label.
  Dynamic Island stays a proper name. Android-owned surfaces resolve these
  resources from Android's locale as before; this phase does not add a separate
  per-app Android locale manager.

## Measured coverage

Run from the repository root:

```sh
PYTHONIOENCODING=utf-8 python tools/check_vietnamese.py
```

Integrated source audit after setup/privacy integration:

| Measurement | Result |
| --- | --- |
| Exact English catalog keys | 497 |
| Catalog values changed / intentionally identical | 491 / 6 |
| Static literal two-argument `tr` calls | 580 |
| Covered static calls | 567 / 580 |
| Distinct static keys covered | 462 / 475 |
| Settings constructor title/short/hint values covered | 37 / 37 |
| Explicit three-argument literal `tr` calls | 84 |
| Remaining interpolated two-argument calls | 8 |
| Vietnamese Android resource names | 4 / 4 |

The six intentionally identical values are `Camera`, `Dynamic Island`,
` · %.1f MB`, `300 m`, `$12.50` and `Anna`. Coverage counts include these
intentional non-translations, not just strings whose text changes.

The catalog includes all existing settings enum labels and hints, including
variable-fed `tr(ru, en)`, `tr(shortRu, shortEn)` and `tr(hintRu, hintEn)` getters.
A separate source audit found those eight Settings getters are the only
nonliteral `tr` call sites outside the localization implementation. No additional
variable-fed UI arrays were found. `LanguageTest` checks enum coverage and
phase-1 permission/service status labels.

The 13 uncovered static calls are intentionally technical copied diagnostic
labels in `LiveParser` (11) and `PayMonitor` (2), such as `title: `,
`no custom layout` and `No amount — not a payment`. They fall back to English
for VI. The payment screen diagnostic explicitly stays English for VI even
though its UI label has a catalog translation.

The eight remaining interpolated calls are technical report/error
strings (`LiveParser`, `PayMonitor`, `PayScreen`, `UpdateChecker`) that deliberately
remain English for VI. The Features schedule/collapse delay, Home's `$app`
duplicate-banner explanation and new setup/privacy UI use explicit Vietnamese.

The inventory is a source audit, not a full Kotlin parser. It counts literal
`tr` arguments (including adjacent concatenated literals) and separately reads
Settings enum constructor string fields. It does not count every raw string,
release note, parser token, native language autonym or externally supplied value.
It detects duplicate catalog keys and lists every missing/interpolated site
instead of hiding English fallback.

## Content boundaries and real limitations

- Notification/message text, names, app labels, lyrics, song/artist titles,
  navigation instructions/streets, payment amounts and remote release notes
  remain supplied content. They are not sent through the catalog.
- Only app-owned labels around supplied values are translated. Demo names,
  currency, units and proper names are preserved; app-owned demo messages and
  navigation explanations have Vietnamese labels.
- Existing English navigation-unit normalization is preserved. Vietnamese
  leaves app-provided units as supplied; this is not a Vietnamese parser
  migration. Clock/navigation/payment recognition still has its existing
  language/token support. Selecting VI does not guarantee recognition of every
  Vietnamese third-party app screen.
- Release notes use the existing English fallback for non-Russian languages.
  Copied technical diagnostics retain stable technical fields and supplied
  values. New or unmatched app labels deliberately fall back to English.
- Language titles remain native autonyms (`Русский`, `English`, `Tiếng Việt`);
  product/platform names (Dynamic Island, Liquid Glass, GitHub, Material You,
  Android, etc.) are not renamed.
- No Android emulator/device visual check, TalkBack check, font-glyph check,
  or live SYSTEM-locale switch has been performed.

## Verification and integration checklist

Localization checks:

- Compiled only pure `Language.kt`, `VietnameseStrings.kt` and `LanguageTest.kt`
  using the cached Kotlin 2.1 JVM compiler and JDK 17; ran JUnit 4.13.2
  `org.junit.runner.JUnitCore com.akira.island.core.LanguageTest`: **8 passed**.
- Coverage inventory: exact counts above, duplicate key check passed.
- Resource XML parse/name parity and `git diff --check`.
- Integrated validation uses the Gradle command in the phase 3 guide.

Completed integration and remaining device checks:

1. `AppLanguage.VI` is added without renaming/removing existing saved enum values.
2. The three interpolation sites have explicit Vietnamese arguments;
   Features' language title uses `tr` rather than `if (Lang.en)`.
3. Compact's short ETA strips the new `Đến lúc ` prefix, and
   the empty action-title fallback uses localized Reply. Supplied action titles
   must remain unchanged.
4. The coverage script and resource parity checks have been rerun after integration.
5. On a device, still check explicit RU/EN/VI,
   SYSTEM EN → VI, VI selected on an English phone, 12/24-hour clocks,
   long settings text, overlay labels and Vietnamese diacritics.
