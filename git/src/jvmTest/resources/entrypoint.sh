#!/bin/sh
set -e

# Create the bare Git repository used by tests.
mkdir -p /srv/git
git init --bare /srv/git/test-repo.git
git config --global init.defaultBranch main
touch /srv/git/test-repo.git/git-daemon-export-ok

# Create the htpasswd file with the test user.
htpasswd -bc /etc/nginx/.htpasswd testuser testpassword

# Ensure the nginx run directory exists.
mkdir -p /run/nginx

# Start fcgiwrap in the background.
fcgiwrap -s unix:/run/fcgiwrap.sock &

# Wait until the socket is available.
for i in $(seq 1 10); do
    [ -S /run/fcgiwrap.sock ] && break
    sleep 0.2
done

# Adjust permissions so nginx can access the socket.
chmod 777 /run/fcgiwrap.sock

# Start nginx in the foreground.
exec nginx -g "daemon off;"
