# Create virtualenv

## Local environment
```sh
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
```

## Setup dev environment
```sh
pip3 install pyspark==3.5.1
```

## Download Jars
```sh
./download_and_copy_jars.sh
```

