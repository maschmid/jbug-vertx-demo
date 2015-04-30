import random
import string

print "<docs>"
for i in range(1024):
    print "<doc>"
    print ''.join(random.choice(string.ascii_lowercase + " ") for _ in range(1024))
    print "</doc>"

print "</docs>"
