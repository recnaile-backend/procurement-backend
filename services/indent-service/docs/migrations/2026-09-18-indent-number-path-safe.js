// Canonical indent-number storage migration.
// MongoDB stores the URL-encoded representation literally.
// Example: IN/26-27/002 -> IN%2F26-27%2F002

// Migrate slash-formatted records.
db.indents.updateMany(
  { indentNumber: { $regex: /^IN\/\d{2}-\d{2}\/\d{3}$/ } },
  [
    {
      $set: {
        indentNumber: {
          $replaceAll: {
            input: "$indentNumber",
            find: "/",
            replacement: "%2F"
          }
        }
      }
    }
  ]
)
