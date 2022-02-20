package me.julb.applications.github.actions;

/**
 * The input branch state. <br>
 * @author Julb.
 */
enum InputTagState {
    /**
     * The branch needs to be created.
     */
    PRESENT,

    /**
     * The branch needs to be deleted.
     */
    ABSENT;
}