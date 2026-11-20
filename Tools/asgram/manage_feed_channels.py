"""Edit the published ASGRAM feed directory for a GitHub repository maintainer."""

import argparse
import json
import re
from pathlib import Path
from urllib.request import urlopen


CATEGORIES = ("News", "Education", "Sports", "አማርኛ")
MAX_CHANNELS = 12


def username_of(value):
    match = re.fullmatch(r"(?:@|https://t\.me/)?([a-z][a-z0-9_]{3,31})/?", value.strip(), re.I | re.ASCII)
    if not match:
        raise ValueError("Enter a public Telegram @username or https://t.me/username link.")
    return match[1].lower()


def verify_public_channel(username):
    with urlopen(f"https://t.me/{username}", timeout=12) as response:
        page = response.read(250_000).decode("utf-8", errors="replace")
    if not re.search(r'class="tgme_page_extra"[^>]*>[^<]*\bsubscribers?\b', page, re.I):
        raise ValueError("Telegram did not return a public broadcast channel for that username.")


def edit_channels(channels, action, username, title, category):
    existing = [entry for entry in channels if entry["username"] != username]
    if action == "remove":
        if len(existing) == len(channels):
            raise ValueError(f"@{username} is not configured.")
        return existing
    title = title.strip()
    if not 1 <= len(title) <= 60 or any(ord(char) < 32 or 127 <= ord(char) <= 159 for char in title):
        raise ValueError("Enter a display name between 1 and 60 characters.")
    if category not in CATEGORIES:
        raise ValueError("Select a category.")
    if len(existing) >= MAX_CHANNELS:
        raise ValueError(f"The Feed supports up to {MAX_CHANNELS} channels.")
    entry = {"username": username, "title": title, "category": category}
    if len(existing) < len(channels):
        return [entry if old["username"] == username else old for old in channels]
    return [*channels, entry]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("add-or-update", "remove"))
    parser.add_argument("channel")
    parser.add_argument("title")
    parser.add_argument("category")
    parser.add_argument("--path", type=Path, default=Path("docs/feed-channels.json"))
    args = parser.parse_args()
    try:
        username = username_of(args.channel)
        config = json.loads(args.path.read_text(encoding="utf-8"))
        updated = edit_channels(config["channels"], args.action, username, args.title, args.category)
        if args.action == "add-or-update":
            verify_public_channel(username)
        args.path.write_text(json.dumps({"channels": updated}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    except (OSError, ValueError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
