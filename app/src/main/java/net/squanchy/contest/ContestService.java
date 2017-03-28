package net.squanchy.contest;

import net.squanchy.remoteconfig.RemoteConfig;
import net.squanchy.service.firebase.FirebaseAuthService;
import net.squanchy.service.firebase.FirebaseDbService;
import net.squanchy.service.firebase.model.FirebaseAchievements;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.schedulers.Schedulers;

import static io.reactivex.Observable.combineLatest;

public class ContestService {

    private final RemoteConfig remoteConfig;
    private final FirebaseDbService dbService;
    private final FirebaseAuthService authService;

    public ContestService(RemoteConfig remoteConfig, FirebaseDbService dbService, FirebaseAuthService authService) {
        this.remoteConfig = remoteConfig;
        this.dbService = dbService;
        this.authService = authService;
    }

    public Observable<ContestStandings> standings() {
        return combineLatest(
                authService.ifUserSignedInThenObservableFrom(dbService::achievements),
                remoteConfig.contestGoal().toObservable(),
                buildStandings()
        ).subscribeOn(Schedulers.io());
    }

    private BiFunction<FirebaseAchievements, Long, ContestStandings> buildStandings() {
        return (achievements, goal) -> ContestStandings.create(goal, achievements.map.size());
    }

    public Observable<ContestStandings> addAchievement(String achievementId) {
        return authService.ifUserSignedInThenCompletableFrom(userId -> dbService.addAchievement(userId, achievementId, System.currentTimeMillis()))
                .andThen(standings())
                .subscribeOn(Schedulers.io());
    }
}
