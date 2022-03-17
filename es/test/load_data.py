#!/usr/bin/env python3

import argparse
import os
import multiprocessing
import requests
import json

from time import time
from pathlib import Path
from random import shuffle


CDR_TYPE = '_doc'
CDR_INDEX = 'cdr_search'

def upload(filepath: Path):
    with open(filepath) as f:
        doc = json.load(f)

    if "document_id" not in doc:
        return

    print('Uploading ' + doc["document_id"])
    headers={"Connection": "close"}
    res = requests.post(f'{args.url}/{CDR_INDEX}/{CDR_TYPE}/{doc["document_id"]}', headers=headers, json=doc, stream=True)
    res.close()


# argument setup
parser = argparse.ArgumentParser(description='Upload CDRs in a directory to Elasticsearch')
parser.add_argument('path', type=str, help='Path to target directory for indexing')
parser.add_argument('--url', type=str, default='http://localhost:9200', help='Endpoint of the Elasticsearch instance')
args = parser.parse_args()

# files to upload
file_queue = []

# track time
start_time = time()

for root_dir, sub_dirs, files in os.walk(args.path):
    for file in files:
        if file.split('.')[-1] == "json" or file.split('.')[-1] == "cdr":
            file_queue.append(Path(root_dir).joinpath(file))

print(f"Uploading {len(file_queue)} files...")

# pool = multiprocessing.Pool(8)
shuffle(file_queue)

# Use the following if multi-threading doesn't work
for file in file_queue:
    upload(file)
# jobs = pool.map(upload, file_queue)

total_time = (time() - start_time) / 60
print(f"Completed in {round(total_time, 2)} minutes")
