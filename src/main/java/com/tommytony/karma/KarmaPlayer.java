package com.tommytony.karma;

public class KarmaPlayer {

    private final Karma karma;
    private final String name;
    private int karmaPoints;
    private long lastActivityTime;
    private long lastGift;
    private KarmaTrack track;

    public KarmaPlayer(Karma karma, String name, int karmaPoints, long lastActivityTime, long lastGift, KarmaTrack track) {
        this.karma = karma;
        this.name = name;
        this.karmaPoints = karmaPoints;
        this.lastActivityTime = lastActivityTime;
        this.lastGift = lastGift;
        this.track = track;
    }

    public void addKarma(int pointsToAdd) {
        if (pointsToAdd > 0) {
            int before = this.karmaPoints;
            this.karmaPoints += pointsToAdd;
            this.karma.checkForPromotion(this, before, this.karmaPoints);
            this.karma.getKarmaDatabase().put(this);
        }
    }

    public void removeKarma(int pointsToRemove) {
        this.removeKarma(pointsToRemove, false);
    }

    public void removeKarmaAutomatic(int pointsToRemove) {
        this.removeKarma(pointsToRemove, true);
    }

    private void removeKarma(int pointsToRemove, boolean automatic) {
        if (pointsToRemove > this.karmaPoints) {
            pointsToRemove = this.karmaPoints;
        }
        if (pointsToRemove > 0) {
            int before = this.karmaPoints;
            this.karmaPoints -= pointsToRemove;
            this.karma.checkForDemotion(this, before, this.karmaPoints, automatic);
            this.karma.getKarmaDatabase().put(this);
        }
    }

    public String getName() {
        return name;
    }

    public int getKarmaPoints() {
        return karmaPoints;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void ping() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void updateLastGiftTime() {
        this.lastGift = System.currentTimeMillis();
    }

    public long getLastGiftTime() {
        return lastGift;
    }

    public boolean canGift() {
        long since = System.currentTimeMillis() - getLastGiftTime();
        return since > 3600 * 1000;
    }

    /**
     * @return the track
     */
    public KarmaTrack getTrack() {
        return track;
    }

    /**
     * @param track the track to set
     */
    public void setTrack(KarmaTrack track) {
        this.track = track;
    }
}
