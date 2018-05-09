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

def onehammer(baseurl, nrequests, l):
    for i in range(nrequests):
        r = requests.get(baseurl + "theweb.test2", params={"a":"aa", "b": "bb", "c": "cc"})
        r.raise_for_status()
        l.append(len(r.text))
        r = requests.post(baseurl + "theweb.test2", params={"a":"aa", "b": "bb", "c": "cc"})
        r.raise_for_status()
        l.append(len(r.text))

def hammer(baseurl, nthread, nrequests):
    l = []
    res = []
    for i in range(nthread):
        t = threading.Thread(target=onehammer, args=(baseurl, nrequests, res))
        l.append(t)
        t.start()
    for t in l:
        t.join()
    assert(len(res) == 2 * nthread * nrequests)

def flextest(baseurl):
    args = {"key1": "val1", "key2": "val2", "key3": "val3"}
    r = requests.post(baseurl + "!theweb.flex", params=args)
    r.raise_for_status()
    res = r.json()
    if not res == args:
        raise Exception("flex result is wrong")

def noargtest(baseurl):
    r = requests.get(baseurl +"theweb.nix")
    r.raise_for_status()
    assert(r.text.rstrip() == "nix")

    r = requests.post(baseurl +"theweb.nix")
    r.raise_for_status()
    assert(r.text.rstrip() == "nix")
    
    
def main():
    parser = argparse.ArgumentParser(description='do some tests')
    parser.add_argument('-port', type=int, default=8888)
    parser.add_argument('-dad', type=str, default="x")
    args = parser.parse_args()
    baseurl = "http://localhost:{0}/dads/{1}/".format(args.port, args.dad)
    noargtest(baseurl)
    simpletest(baseurl)
    flextest(baseurl)
    hammer(baseurl, 5, 10)

main()
