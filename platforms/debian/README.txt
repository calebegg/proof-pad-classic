To create a Debian package for easy installation, download the appropriate ACL2 version (from http://acl2s.ccs.neu.edu/acl2s/update/images/) and extract it to the directory './proofpad/var/lib/', renaming the resulting folder to 'acl2'.  Next, copy the Proof Pad jar file into './proofpad/var/lib'. Finally, in the folder 'debian', run the following command:

$ dpkg --build proofpad InstallProofPad<version>.deb

and replace <version> with 32 or 64 as appropriate. This will create the final installation package.