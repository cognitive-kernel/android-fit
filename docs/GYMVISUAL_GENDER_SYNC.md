# GymVisual gender synchronization

The exercise catalog uses GymVisual's published exercise reference workbook:
https://docs.google.com/spreadsheets/d/1dUMCUSk8rqPoLZ5Xltg7shqgfqyHfALq/edit

GymVisual documents that the reference ID is the leading numeric portion of its media filename. In this repository, the catalog asset ID keeps the two-digit media suffix, so the synchronizer resolves the workbook ID using the asset ID without its final two digits.

## Run

From the repository root:

```bash
python scripts/sync_gymvisual_gender.py
```

The default is a dry run. Review the matched/unmatched counts and sample rename plan. To apply:

```bash
python scripts/sync_gymvisual_gender.py --apply
```

The script:
- reads only the official workbook's ID and Gender columns;
- updates only exact reference-ID matches with a valid Male/Female value;
- preserves the numeric asset ID and renames the actual GIF to include its verified gender;
- updates the catalog name, gender and filename together;
- leaves unmatched records unchanged;
- preflights missing files and filename collisions before applying changes.

The SQLite index is rebuilt on the next app launch through the database version bump. Do not manually edit the generated database.

## Validation and delivery

After applying, run the Android unit tests and release build. Check that every catalog file exists, every ID is unique, every renamed filename includes its catalog gender, and no unmatched record was altered. Keep GymVisual attribution and comply with the media license/permission applicable to this repository before distributing the APK.

The workbook assigns a reference gender to each exercise record; it is not a claim that the movement itself is biologically exclusive to that gender. The app should present this as the gender of the illustrated reference asset.
