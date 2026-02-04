#!/bin/zsh

echo "Date (Local),Commits,Files Changed,Lines Added,Lines Deleted" > git_stats.csv

git log --date=local --pretty=format:%ad --author="$(git config user.name)" |
  sort | uniq -c | while read count date; do
    # Commits = $count, Date = $date (now local format)
    
    # Files changed, +lines, -lines for this day (local)
    stats=$(git log --author="$(git config user.name)" \
            --since="$date 00:00" --until="$date 23:59" \
            --date=local --pretty=tformat: --shortstat 2>/dev/null | \
            awk '
      /files? changed/ {files += $1}
      /insertion/     {add += $1}
      /deletion/      {del += $1}
      END {printf "%d,%d,%d", files, add, del}
    ')
    
    echo "$date,$count,$stats" >> git_stats.csv
done

cat git_stats.csv
echo "--- Time estimate ---"
git-hours | head -5
