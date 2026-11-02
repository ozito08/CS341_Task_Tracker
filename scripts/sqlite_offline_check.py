#!/usr/bin/env python3
"""Independent SQLite engine checks; does NOT claim JDBC or JUnit success."""
from pathlib import Path
from contextlib import closing
import sqlite3, json, tempfile, datetime
root=Path(__file__).resolve().parent.parent
schema=(root/'sql/schema.sql').read_text(encoding='utf-8')
checks=[]
def record(name,func):
    try: func();checks.append({'name':name,'status':'PASS'})
    except Exception as e: checks.append({'name':name,'status':'FAIL','reason':str(e)})
with tempfile.TemporaryDirectory() as tmp:
    db=Path(tmp)/'test.db'
    con=sqlite3.connect(db)
    con.execute('PRAGMA foreign_keys=ON')
    record('sqlite foreign key enabled',lambda: (_ for _ in ()).throw(AssertionError('FK off')) if con.execute('PRAGMA foreign_keys').fetchone()[0]!=1 else None)
    record('schema first initialization',lambda: con.executescript(schema))
    record('schema idempotency',lambda: con.executescript(schema))
    def schema_fields():
        names={r[1] for r in con.execute('PRAGMA table_info(tasks)')}
        assert {'id','creator_id','assignee_id','title','description','completed','priority','due_date','created_at'}.issubset(names)
        assert len(list(con.execute('PRAGMA foreign_key_list(tasks)')))==2
        assert 'ix_tasks_due' in {r[1] for r in con.execute('PRAGMA index_list(tasks)')}
    record('schema fields, foreign key and indexes',schema_fields)
    def users_and_role_vis():
        con.execute("INSERT INTO users(username,password_hash) VALUES('Alice','hash1'),('Bob','hash2'),('Carol','hash3')")
        con.execute("INSERT INTO tasks(creator_id,assignee_id,title,priority,due_date) VALUES(1,2,'Example','HIGH','2026-12-01')")
        counts=[con.execute('SELECT count(*) FROM tasks WHERE creator_id=? OR assignee_id=?',(n,n)).fetchone()[0] for n in (1,2,3)]
        assert counts==[1,1,0],counts
    record('creator / assignee / third-party visibility',users_and_role_vis)
    def forbidden_fk():
        try: con.execute("INSERT INTO tasks(creator_id,assignee_id,title) VALUES(1,999,'invalid')")
        except sqlite3.IntegrityError: return
        raise AssertionError('foreign key not enforced')
    record('invalid foreign key rejected',forbidden_fk)
    def duplicate_username():
        try: con.execute("INSERT INTO users(username,password_hash) VALUES('ALICE','hash')")
        except sqlite3.IntegrityError: return
        raise AssertionError('NOCASE UNIQUE not enforced')
    record('case insensitive username uniqueness',duplicate_username)
    def priority_check():
        try: con.execute("INSERT INTO tasks(creator_id,assignee_id,title,priority) VALUES(1,1,'invalid','URGENT')")
        except sqlite3.IntegrityError: return
        raise AssertionError('priority CHECK not enforced')
    record('priority CHECK',priority_check)
    def unauthorized_update():
        cur=con.execute("UPDATE tasks SET title='hacked' WHERE id=1 AND creator_id=3")
        assert cur.rowcount==0
        cur=con.execute("UPDATE tasks SET completed=1-completed WHERE id=1 AND (creator_id=2 OR assignee_id=2)")
        assert cur.rowcount==1
        assert con.execute('SELECT completed,title FROM tasks WHERE id=1').fetchone()==(1,'Example')
    record('conditional update authorization patterns',unauthorized_update)
    con.commit();con.close()
    def persistence():
        with closing(sqlite3.connect(db)) as new:
            with new:
                assert new.execute('SELECT count(*) FROM users').fetchone()[0]==3
                assert new.execute('SELECT count(*) FROM tasks').fetchone()[0]==1
    record('file-based persistence after reopen',persistence)
status='PASS' if all(c['status']=='PASS' for c in checks) else 'FAIL'
report={'date':datetime.date.today().isoformat(),'verification':'Python stdlib sqlite3 only (not Java JDBC/JUnit)','status':status,'checks':checks}
(root/'docs/SQLITE_OFFLINE_RESULTS.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for c in checks:print(c['status']+': '+c['name'])
print('RESULT:',status,'(' + str(len(checks)) + ' checks )')
raise SystemExit(0 if status=='PASS' else 1)
