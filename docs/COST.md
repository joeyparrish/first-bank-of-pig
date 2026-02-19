# Firebase Cost Estimates

## Services Used

| Service | Purpose | Pricing Model |
|---------|---------|---------------|
| Firebase Auth | Google Sign-In (parents), anonymous auth (kids) | Free for all providers used |
| Cloud Firestore | All app data (families, children, transactions, devices) | Free tier + pay per operation |
| Cloud Functions | Weekly cleanup of expired codes | Requires Blaze plan, negligible cost |
| Firebase Analytics | User engagement metrics | Free |
| Firebase Crashlytics | Crash reporting | Free |

## Usage Assumptions (Per Family)

- 1-2 parents, 1-3 kids
- Parent opens app ~1x/day, adds ~2-3 transactions/week
- Each kid device opens app ~1x/day with real-time listener
- Average family accumulates ~100 transactions over time

## Daily Firestore Operations Per Family

| Action | Reads | Writes |
|--------|-------|--------|
| Parent opens app (family + children + transactions) | ~45 | 0 |
| Kid devices open app (access check + data + lastAccessed) | ~50 | 2 |
| Parent adds transaction (~0.3/day average) | 0 | ~0.3 |
| Real-time listener updates | ~10 | 0 |
| **Daily total per family** | **~105** | **~2.3** |

## Free Tier Limits (Spark / Blaze)

Both the Spark (free) and Blaze (pay-as-you-go) plans include these daily
free allowances. The Blaze plan has no base cost.

| Resource | Free Allowance | Per-Family Daily | Families Supported |
|----------|---------------|-------------------|-------------------|
| Reads | 50K/day | ~105 | **~475** |
| Writes | 20K/day | ~2.3 | ~8,700 |
| Deletes | 20K/day | negligible | not a bottleneck |
| Storage | 1 GiB | ~15 KB/family | ~70,000 |
| Auth | unlimited | - | unlimited |

**Bottleneck is reads: ~400-500 families for free.**

## Marginal Cost After Free Tier (Blaze Plan)

| Resource | Price | Per-Family/Month |
|----------|-------|-----------------|
| Reads | $0.06 / 100K | $0.0019 |
| Writes | $0.18 / 100K | $0.0001 |
| Storage | $0.18 / GiB/mo | negligible |
| **Total** | | **~$0.002/family/month** |

Roughly a fifth of a cent per family per month after the free tier.

## Projected Monthly Costs

| Scale | Monthly Cost |
|-------|-------------|
| 1-500 families | $0 (free tier) |
| 1,000 families | ~$1 |
| 5,000 families | ~$10 |
| 10,000 families | ~$20 |

## Notes

- Cloud Functions require the Blaze plan but cost effectively nothing (weekly
  cleanup job: 52 invocations/year).
- On the Spark plan, run cleanup locally instead: `cd functions && node cleanup.js`
- Firebase Analytics and Crashlytics are free at any scale.
- Auth is free for Google Sign-In and anonymous auth at any scale.
- Real-time listeners are the biggest variable — if kids leave the app open
  continuously, read counts could be higher than estimated.

## Monitoring Usage

Run `./scripts/firebase-usage.sh` to check current Firestore usage, or view
the Firebase Console under Firestore > Usage for graphical dashboards.
