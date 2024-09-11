gawk -vFPAT='([^ ]*)|(\"[^\"]+\")' -f convert_pgn_to_json.awk $1.pgn
