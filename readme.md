# Create virtualenv

## Local environment
```sh
python3 -m venv venv
. venv/bin/activate
pip install --upgrade pip
```

## Setup dev environment
```sh

pip3 install pylint
pylint --generate-rcfile > .pylintrc
```

```sh
# Mysql DB deployment
docker-compose up -d

# Connect through an IDE
host: localhost
port: 3306
user: root
pass: password
```