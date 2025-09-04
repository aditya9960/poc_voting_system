# poc_voting_system
POC - Feature Voting System – users post a feature and upvote others

## Setup Backend

```bash
$env:FLASK_APP = "run.py"
pip install -r requirements_txt.txt
```

### DB
```bash
cd .. poc_voting_system/backend
flask db init
flask db migrate -m "Initial migration"
flask db upgrade
```

### Run
```bash
python run.py
```