# simple python tests
import argparse
import requests
import threading

def simpletest(baseurl):
    r = requests.get(baseurl + "theweb.test2", params={"a":"aa", "b": "bb", "c": "cc"})
    r.raise_for_status()
    print("GET")
    print(r.text)
    r = requests.post(baseurl + "theweb.test2", params={"a":"aa", "b": "bb", "c": "cc"})
    r.raise_for_status()
    print("POST")
    print(r.text)

def onehammer(baseurl, nrequests):
    l = []
    for i in range(nrequests):
        r = requests.get(baseurl + "theweb.test2", params={"a":"aa", "b": "bb", "c": "cc"})
        r.raise_for_status()
        l.append(len(r.text))
        r = requests.post(baseurl + "theweb.test2", params={"a":"aa", "b": "bb", "c": "cc"})
        r.raise_for_status()
        l.append(len(r.text))

def hammer(baseurl, nthread, nrequests):
    l = []
    for i in range(nthread):
        t = threading.Thread(target=onehammer, args=(baseurl, nrequests))
        l.append(t)
        t.start()
    for t in l:
        t.join()

def main():
    parser = argparse.ArgumentParser(description='do some tests')
    parser.add_argument('-port', type=int, default=8888)
    parser.add_argument('-dad', type=str, default="x")
    args = parser.parse_args()
    baseurl = "http://localhost:{0}/dads/{1}/".format(args.port, args.dad)
    simpletest(baseurl)
    hammer(baseurl, 5, 1000)

main()
