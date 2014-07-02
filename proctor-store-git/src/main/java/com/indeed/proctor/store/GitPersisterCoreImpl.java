package com.indeed.proctor.store;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

public class GitPersisterCoreImpl implements GitPersisterCore, Closeable {
    private static final Logger LOGGER = Logger.getLogger(GitPersisterCoreImpl.class);

    private final String gitUrl;
    private final Repository repo;
    private final File tempDir;

    /**
     * @param gitUrl
     * @param username
     * @param password
     * @param tempDir
     * @param
     */
    public GitPersisterCoreImpl(final String gitUrl, final String username, final String password, final File tempDir) {
        /*
         * Eventually, make a temp dir, clone the github repo, then delete temp dir when done
         */
        String localPath = "/Users/jcheng/Documents/git/abcabctest"; // change to tempDir
        //this.gitUrl = gitUrl;
        this.tempDir = tempDir;
        this.gitUrl = "http://github.wvrgroup.internal/jcheng/git-proctor-test-definitions.git";

        UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(username, password);
        Git git = null;
        try {
            git = Git.open(new File(localPath));
        } catch (IOException e) {
            //System.err.println("Could not open directory at " + localPath, e);
            System.out.println("Error when opening git\n\n");
            e.printStackTrace();
        }
        this.repo = git.getRepository();
        // CLONING A GITHUB REPO
        /*
        CloneCommand clone = git.cloneRepository();
        clone.setBare(false);
        clone.setCloneAllBranches(true);
        clone.setDirectory(new File(localPath)).setURI(gitUrl);
        clone.setCredentialsProvider(user);
        git = clone.call();
        */

        try {
            System.out.println(git.branchList().call());
            for (Ref ref : git.branchList().call()) {
                System.out.println(ref.getName());
            }

            /***********
             * try and read a file from github
             ***********/
            // find the HEAD
            ObjectId lastCommitId = repo.resolve(Constants.HEAD);

            // a RevWalk allows to walk over commits based on some filtering that is defined
            RevWalk revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            // and using commit's tree find the path
            RevTree tree = commit.getTree();
            System.out.println("Having tree: " + tree);

            // now try to find a specific file
            TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create("README.md"));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file 'README.md'");
            }

            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repo.open(objectId);

            // and then one can the loader to read the file
            loader.copyTo(System.out);

            repo.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public Repository getRepo() {
        return repo;
    }

    @Override
    public String getGitUrl() {
        return gitUrl;
    }

    @Override
    public boolean cleanUserWorkspace(String username) {
        return false; // TODO
    }

    @Override
    public <C> C getFileContents(Class<C> c, String[] path, C defaultValue, long revision)
            throws StoreException.ReadException, JsonProcessingException {
        System.out.println("\nCall to getFileContents which will cause a nullpointerex\n");
        return null; // TODO
    }

    static class GitRcsClient implements FileBasedProctorStore.RcsClient {
        private final Git git;

        public GitRcsClient(Git git) {
            this.git = git;
        }

        @Override
        public void add(File file) throws Exception {
            git.add().addFilepattern(".").call(); //TODO
        }

        @Override
        public void delete(File testDefinitionDirectory) throws Exception {
            //TODO
        }
    }

    @Override
    public void doInWorkingDirectory(String username, String password, String comment, long previousVersion,
            FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        System.out.println("tempDir name: " + tempDir.getName());
        System.out.println("tempDir path: " + tempDir.getAbsolutePath());
        System.out.println("username: " + username);
        System.out.println("comment: " + comment);
        String localPath = "/Users/jcheng/Documents/git/abcabctest"; // TODO shouldn't be hardcoded here
        String gitURI = "http://github.wvrgroup.internal/jcheng/git-proctor-test-definitions.git"; //TODO shouldn't be hardcoded here

        UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(username, password);
        Git git = null; // TODO fix try catch structure here
        try {
            git = Git.open(new File(localPath));
            System.out.println(git.branchList().call());
            for (Ref ref : git.branchList().call()) {
                System.out.println(ref.getName());
                //request.setAttribute("bob", ref.getName());//doesn't work
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            final FileBasedProctorStore.RcsClient rcsClient = new GitPersisterCoreImpl.GitRcsClient(git);
            System.out.println(git.getRepository().getDirectory().getAbsolutePath());
            System.out.println(git.getRepository().getDirectory().getName());
            final boolean thingsChanged = updater.doInWorkingDirectory(rcsClient, new File(localPath));

            if (thingsChanged) {
                System.out.println("things changed! now on to committing and pushing all files"); //TODO
                rcsClient.add(null);
                git.commit().setMessage(comment).call();
                git.push().setCredentialsProvider(user).call();

            }
        } catch (Exception e) {
            System.out.println("error while messing with rcsClient " + e);
        }

        // CLONING A GITHUB REPO
            /*
            CloneCommand clone = git.cloneRepository();
            clone.setBare(false);
            clone.setCloneAllBranches(true);
            clone.setDirectory(new File(localPath)).setURI(gitURI);
            clone.setCredentialsProvider(user);
            git = clone.call();
            */

    }

    @Override
    public FileBasedProctorStore.TestVersionResult determineVersions(long fetchRevision)
            throws StoreException.ReadException {
        return new FileBasedProctorStore.TestVersionResult(
                Collections.<FileBasedProctorStore.TestVersionResult.Test>emptyList(),
                new Date(),
                "dummyAuthor",
                0,
                "dummydesccommitmessage"
        ); // TODO
    }

    @Override
    public void close() throws IOException {
        // TODO
    }
}