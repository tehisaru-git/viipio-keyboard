# Pull Request

## Description

<!-- Provide a clear and concise description of your changes -->

## Type of Change

<!-- Mark the relevant option with an 'x' -->

- [ ] 🐛 Bug fix (non-breaking change which fixes an issue)
- [ ] ✨ New feature (non-breaking change which adds functionality)
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] 📝 Documentation update
- [ ] 🎨 Style/UI change (formatting, visual improvements)
- [ ] ♻️ Code refactoring (no functional changes)
- [ ] ⚡ Performance improvement
- [ ] ✅ Test update/addition
- [ ] 🔧 Build/configuration change

## Related Issues

<!-- Link to related issues using #issue_number -->

Closes #
Related to #

## Changes Made

<!-- Detailed list of changes made in this PR -->

-
-
-

## Testing Performed

### Manual Testing

<!-- Describe how you tested these changes -->

**Test Device:**
- Device: <!-- e.g., Pixel 8 -->
- Android Version: <!-- e.g., Android 14 -->
- Viipio version: <!-- e.g., v0.6.0 -->

**Test Scenarios:**
- [ ] <!-- Test scenario 1 -->
- [ ] <!-- Test scenario 2 -->
- [ ] <!-- Test scenario 3 -->

**Results:**
<!-- Describe test results -->

### Automated Testing

```bash
# Commands run
python3 scripts/viipio-check.py quick
python3 scripts/viipio-check.py targeted
```

- [ ] Scoped tests pass; full-suite failures and baseline comparison are disclosed
- [ ] Relevant installed-editor and privacy paths were checked (or gaps documented)
- [ ] No new lint warnings
- [ ] Build succeeds

## Screenshots/Videos

<!-- If applicable, add screenshots or videos demonstrating the changes -->

**Before:**
<!-- Screenshot/description of behavior before changes -->

**After:**
<!-- Screenshot/description of behavior after changes -->

## Performance Impact

<!-- Describe any performance implications -->

- **Memory**: <!-- e.g., No change, +5MB, -10MB -->
- **CPU**: <!-- e.g., No change, slight increase, optimized -->
- **Battery**: <!-- e.g., No impact, minimal impact -->
- **Latency**: <!-- e.g., No change, reduced by 20ms -->

## Privacy & Security Checklist

<!-- CRITICAL: All privacy/security items must be checked -->

- [ ] ✅ No network code added
- [ ] ✅ Any diagnostic changes use explicit, separate consent and exclude sensitive fields
- [ ] ✅ No third-party SDKs added (except ONNX Runtime if needed)
- [ ] ✅ All data processing remains local
- [ ] ✅ No sensitive data logging
- [ ] ✅ No new permissions required
- [ ] ✅ User privacy maintained

**Privacy Impact:** <!-- None / Minimal / Moderate / Significant -->
**Explanation:** <!-- If any privacy impact, explain here -->

## Code Quality Checklist

- [ ] My code follows the project's style guidelines
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] Scoped tests pass and any inherited broad-suite failures are reported honestly
- [ ] Any dependent changes have been merged and published

## Documentation

- [ ] README.md updated (if needed)
- [ ] User Manual updated (if user-facing changes)
- [ ] FAQ updated (if common questions addressed)
- [ ] Code comments added/updated
- [ ] CHANGELOG.md updated

## Breaking Changes

<!-- If this is a breaking change, describe the impact and migration path -->

**Impact:**
<!-- Who is affected and how? -->

**Migration Path:**
<!-- How can users adapt to this change? -->

## Additional Context

<!-- Add any other context, considerations, or notes about the pull request -->

## Checklist for Maintainers

<!-- Maintainers will check these before merging -->

- [ ] Code review completed
- [ ] Tests verified on multiple devices
- [ ] Documentation reviewed
- [ ] No privacy violations
- [ ] Performance acceptable
- [ ] Breaking changes documented
- [ ] CHANGELOG.md updated
- [ ] Ready to merge

---

## PR Guidelines

**Before submitting:**
1. Ensure your branch is up-to-date with main
2. Run all tests locally
3. Check for merge conflicts
4. Fill out all relevant sections above

**After submitting:**
1. Respond to review comments promptly
2. Push updates to the same branch
3. Mark conversations as resolved when addressed
4. Be patient and respectful

**Review process:**
- PRs are typically reviewed within 48 hours
- Complex changes may require multiple rounds of review
- Breaking changes require extra scrutiny
- Privacy-related changes require careful review

**Questions?**
- See the [Viipio contribution overview](../CONTRIBUTING.md) and [quality gates](../docs/development/viipio-quality-gates.md); the archived CleverKeys section is historical
- Ask in PR comments if anything is unclear
- Check the [Viipio repository](https://github.com/tehisaru-git/viipio-keyboard) for current project links

---

**Thank you for contributing to Viipio keyboard!** 🎉

🧠 **Think Faster** • ⌨️ **Type Smarter** • 🔒 **Stay Private**
