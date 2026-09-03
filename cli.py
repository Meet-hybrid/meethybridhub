#!/usr/bin/env python3
import os
import sys
from openai import OpenAI

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 cli.py \"Your instructions here\" [filename to save]")
        sys.exit(1)

    prompt = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None

    client = OpenAI(
        api_key="sk-FgtMOFs4p1MsMlCTxwl5LRVWYx0wmbVYxmaluv4NqAZVxINx",
        base_url="http://localhost:8318/v1"
    )

    system_prompt = (
        "You are an expert software engineer CLI assistant. "
        "When the user asks you to write code, output ONLY the raw code or "
        "enclosed cleanly so it can be written directly to a file if needed. "
        "Do not include unnecessary conversational filler if code is requested."
    )

    print("🤖 Generating response from Claude Opus 5...")
    
    try:
        response = client.chat.completions.create(
            model="claude-opus-5",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": prompt}
            ]
        )
        content = response.choices[0].message.content

        print("\n--- Output ---")
        print(content)
        print("--------------\n")

        if output_file:
            # Clean up markdown code blocks if present when saving to a file
            if content.startswith("```"):
                lines = content.splitlines()
                # Remove first and last line if they are markdown code ticks
                if lines[0].startswith("```") and lines[-1].startswith("```"):
                    content = "\n".join(lines[1:-1])

            with open(output_file, "w") as f:
                f.write(content)
            print(f"✅ Successfully saved output to {output_file}")

    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()
