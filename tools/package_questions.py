#!/usr/bin/env python3
"""Validate the editable question files and create a GitHub content archive."""

import argparse
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
QUESTION_DIR = ROOT / "questions"
SCHEMA_VERSION = 1
EXPECTED_FILES = {
    "particles.json",
    "vocabulary.json",
    "verbs.json",
    "conjugation.json",
}
CATEGORIES = {"VOCABULARY", "PARTICLE", "VERB", "GRAMMAR_MARKER"}
VERSION_PATTERN = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def text(value, field):
    require(isinstance(value, str) and value.strip(), f"{field} must be a non-empty string")


def choices(question, answer_field, label):
    options = question.get("choices")
    require(isinstance(options, list) and len(options) == 6, f"{label} must have six choices")
    require(all(isinstance(option, str) and option.strip() for option in options),
            f"{label} choices must be non-empty strings")
    require(len(set(options)) == 6, f"{label} choices must be unique")
    require(question.get(answer_field) in options, f"{label} answer must be one of its choices")


def tokens(question, label):
    items = question.get("tokens")
    require(isinstance(items, list) and items, f"{label} must contain Japanese tokens")
    for index, item in enumerate(items):
        require(isinstance(item, dict), f"{label} token {index} must be an object")
        for field in ("japanese", "hiragana", "french"):
            text(item.get(field), f"{label} token {index} {field}")
        require(item.get("category") in CATEGORIES,
                f"{label} token {index} has an unknown category")
    return items


def validate_bank(filename, questions):
    require(isinstance(questions, list) and questions, f"{filename} must contain questions")
    ids = set()
    for index, question in enumerate(questions):
        label = f"{filename} question {index}"
        require(isinstance(question, dict), f"{label} must be an object")
        for field in ("id", "topic"):
            text(question.get(field), f"{label} {field}")
        for field in ("level", "source"):
            if field in question:
                text(question[field], f"{label} {field}")
        require(question["id"] not in ids, f"{filename} contains duplicate id {question['id']}")
        ids.add(question["id"])

        if filename in ("vocabulary.json", "verbs.json"):
            for field in ("kanji", "hiragana"):
                text(question.get(field), f"{label} {field}")
            text(question.get("answer"), f"{label} answer")
            choices(question, "answer", label)
        else:
            question_tokens = tokens(question, label)
            text(question.get("translation"), f"{label} translation")
            text(question.get("explanation"), f"{label} explanation")
            if filename == "particles.json":
                index = question.get("missingTokenIndex")
                require(isinstance(index, int) and not isinstance(index, bool) and
                        0 <= index < len(question_tokens), f"{label} has an invalid missingTokenIndex")
                require(question_tokens[index]["category"] in {"PARTICLE", "GRAMMAR_MARKER"},
                        f"{label} missing token must be a particle or grammar marker")
                text(question.get("answer"), f"{label} answer")
                require(question_tokens[index]["japanese"] == question["answer"],
                        f"{label} missing token must match its answer")
                choices(question, "answer", label)
            else:
                text(question.get("contextFrench"), f"{label} contextFrench")
                text(question.get("punctuation"), f"{label} punctuation")
                index = question.get("verbTokenIndex")
                require(isinstance(index, int) and not isinstance(index, bool) and
                        0 <= index < len(question_tokens), f"{label} has an invalid verbTokenIndex")
                require(question_tokens[index]["category"] == "VERB",
                        f"{label} verbTokenIndex must point to a verb")
                text(question.get("answerEnding"), f"{label} answerEnding")
                choices(question, "answerEnding", label)


def load_questions():
    manifest_path = QUESTION_DIR / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    require(manifest.get("schemaVersion") == SCHEMA_VERSION,
            f"Unsupported schemaVersion in {manifest_path}")
    version = manifest.get("contentVersion")
    require(isinstance(version, str) and VERSION_PATTERN.fullmatch(version),
            "contentVersion must use MAJOR.MINOR.PATCH")
    files = manifest.get("files")
    require(isinstance(files, list) and len(files) == len(EXPECTED_FILES) and
            set(files) == EXPECTED_FILES, "manifest.json must list the four question files exactly once")
    data = {}
    for filename in files:
        path = QUESTION_DIR / filename
        questions = json.loads(path.read_text(encoding="utf-8"))
        validate_bank(filename, questions)
        data[filename] = questions
    require(
        len(set(question["id"] for bank in data.values() for question in bank)) ==
        sum(len(bank) for bank in data.values()),
        "Question IDs must be unique across all games"
    )
    return manifest, files


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=ROOT / "build" / "questions.zip")
    parser.add_argument("--expected-version")
    args = parser.parse_args()

    manifest, files = load_questions()
    if args.expected_version:
        expected = args.expected_version.removeprefix("questions-v")
        require(manifest["contentVersion"] == expected,
                "Git tag version must match manifest contentVersion")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(QUESTION_DIR / "manifest.json", "manifest.json")
        for filename in files:
            archive.write(QUESTION_DIR / filename, filename)
    print(f"Validated content v{manifest['contentVersion']} and wrote {args.output}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, json.JSONDecodeError, zipfile.BadZipFile) as error:
        raise SystemExit(f"Question data validation failed: {error}") from error
