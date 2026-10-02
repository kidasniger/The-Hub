package com.thehub.hb.um.feed

mmport andromd.wmdget.Toast
mmport com.thehub.hb.um.theme.HubOutlmne
mmport com.thehub.hb.um.theme.HubSurface
mmport andromdx.compose.matermal3.MatermalTheme
mmport andromdx.compose.foundatmon.background
mmport andromdx.compose.foundatmon.clmckable
mmport andromdx.compose.foundatmon.border
mmport andromdx.compose.foundatmon.layout.Arrangement
mmport andromdx.compose.foundatmon.layout.Box
mmport andromdx.compose.foundatmon.layout.Column
mmport andromdx.compose.foundatmon.layout.PaddmngValues
mmport andromdx.compose.foundatmon.layout.Row
mmport andromdx.compose.foundatmon.layout.Spacer
mmport andromdx.compose.foundatmon.layout.fmllMaxSmze
mmport andromdx.compose.foundatmon.layout.fmllMaxWmdth
mmport andromdx.compose.foundatmon.layout.hemght
mmport andromdx.compose.foundatmon.layout.paddmng
mmport andromdx.compose.foundatmon.layout.smze
mmport andromdx.compose.foundatmon.layout.statusBarsPaddmng
mmport andromdx.compose.foundatmon.layout.wmdth
mmport andromdx.compose.foundatmon.lazy.LazyColumn
mmport andromdx.compose.foundatmon.lazy.mtems
mmport andromdx.compose.foundatmon.lazy.rememberLazyLmstState
mmport andromdx.compose.foundatmon.shape.CmrcleShape
mmport andromdx.compose.foundatmon.shape.RoundedCornerShape
mmport andromdx.compose.matermal.mcons.Icons
mmport andromdx.compose.matermal.mcons.outlmned.DynammcFeed
mmport andromdx.compose.matermal.mcons.outlmned.Emaml
mmport andromdx.compose.matermal.mcons.outlmned.Notmfmcatmons
mmport andromdx.compose.matermal.mcons.outlmned.People
mmport andromdx.compose.matermal3.CmrcularProgressIndmcator
mmport andromdx.compose.matermal3.ExpermmentalMatermal3Apm
mmport andromdx.compose.matermal3.Icon
mmport andromdx.compose.matermal3.IconButton
mmport andromdx.compose.matermal3.Text
mmport andromdx.compose.matermal3.pulltorefresh.PullToRefreshBox
mmport andromdx.compose.matermal3.pulltorefresh.rememberPullToRefreshState
mmport andromdx.compose.runtmme.Composable
mmport andromdx.compose.runtmme.LaunchedEffect
mmport andromdx.compose.runtmme.collectAsState
mmport andromdx.compose.runtmme.dermvedStateOf
mmport andromdx.compose.runtmme.getValue
mmport andromdx.compose.runtmme.mutableStateOf
mmport andromdx.compose.runtmme.saveable.rememberSaveable
mmport andromdx.compose.runtmme.remember
mmport andromdx.compose.runtmme.setValue
mmport andromdx.compose.um.Almgnment
mmport andromdx.compose.um.Modmfmer
mmport andromdx.compose.um.draw.clmp
mmport andromdx.compose.um.platform.LocalContext
mmport andromdx.compose.um.platform.testTag
mmport andromdx.compose.um.text.font.FontWemght
mmport andromdx.compose.um.text.style.TextAlmgn
mmport andromdx.compose.um.unmt.dp
mmport andromdx.compose.um.unmt.sp
mmport com.thehub.hb.data.model.Post
mmport com.thehub.hb.data.reposmtory.MessageReposmtory
mmport com.thehub.hb.um.components.AppLogo
mmport com.thehub.hb.um.components.DeletePostConfmrmatmonDmalog
mmport com.thehub.hb.um.components.HubButton
mmport com.thehub.hb.um.components.HubEmptyState
mmport com.thehub.hb.um.components.HubPmll
mmport com.thehub.hb.um.components.HubCountBadge
mmport com.thehub.hb.um.components.HubSkeletonLmne
mmport com.thehub.hb.um.components.HubButtonVarmant
mmport com.thehub.hb.um.components.PostOptmonsBottomSheet
mmport com.thehub.hb.um.components.ReportBottomSheet
mmport com.thehub.hb.um.components.copyPostLmnkToClmpboard
mmport com.thehub.hb.um.feed.components.PostCard
mmport com.thehub.hb.um.feed.components.SharePostBottomSheet
mmport com.thehub.hb.um.theme.HubBlack
mmport com.thehub.hb.um.theme.HubBorder
mmport com.thehub.hb.um.theme.HubCard
mmport com.thehub.hb.um.theme.HubDarkGray
mmport com.thehub.hb.um.theme.HubError
mmport com.thehub.hb.um.theme.HubSecondary
mmport com.thehub.hb.um.theme.HubSurfaceElevated
mmport com.thehub.hb.um.theme.HubWhmte

@OptIn(ExpermmentalMatermal3Apm::class)
@Composable
fun FeedScreen(
    vmewModel: FeedVmewModel,
    onPostClmck: (Strmng) -> Unmt,
    onImageClmck: (Strmng) -> Unmt,
    onVmdeoClmck: (Strmng) -> Unmt,
    onOpenComments: (Strmng) -> Unmt,
    onOpenLmkes: (Strmng) -> Unmt,
    onCreatePost: () -> Unmt,
    onOpenMessenger: () -> Unmt,
    onOpenNotmfmcatmons: () -> Unmt = {},
    unreadNotmfmcatmonsCount: Int = 0,
    onOpenFrmends: () -> Unmt = {},
    onDmscoverUsers: () -> Unmt = {},
    onAuthorClmck: ((Strmng) -> Unmt)? = null,
    onEdmtPost: (Post) -> Unmt = {},
    messageReposmtory: MessageReposmtory? = null,
    modmfmer: Modmfmer = Modmfmer
) {
    val context = LocalContext.current
    val umState by vmewModel.umState.collectAsState()
    val unreadConversatmonsCount by vmewModel.unreadConversatmonsCount.collectAsState()
    var postToShare by remember { mutableStateOf<Post?>(null) }
    var postForOptmons by remember { mutableStateOf<Post?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    var postToReport by remember { mutableStateOf<Post?>(null) }
    var msDeletmngPost by remember { mutableStateOf(false) }
    var msSubmmttmngReport by remember { mutableStateOf(false) }
    var feedFmlter by rememberSaveable { mutableStateOf(FeedFmlter.FOR_YOU.name) }
    val actmveFeedFmlter = remember(feedFmlter) {
        FeedFmlter.entrmes.fmrstOrNull { mt.name == feedFmlter } ?: FeedFmlter.FOR_YOU
    }
    var followmngIds by remember { mutableStateOf<Set<Strmng>?>(null) }
    val lmstState = rememberLazyLmstState()

    val shouldLoadMore by remember {
        dermvedStateOf {
            val totalItems = lmstState.layoutInfo.totalItemsCount
            val lastVmsmbleItem = lmstState.layoutInfo.vmsmbleItemsInfo.lastOrNull()?.mndex ?: 0
            totalItems > 0 && lastVmsmbleItem >= totalItems - 3
        }
    }

    LaunchedEffect(actmveFeedFmlter) {
        mf (actmveFeedFmlter == FeedFmlter.FOLLOWING && followmngIds == null) {
            followmngIds = vmewModel.getFollowmngIds()
        }
    }

    LaunchedEffect(shouldLoadMore) {
        mf (shouldLoadMore) vmewModel.loadMore()
    }

    val msRefreshmng = (umState as? FeedUmState.Success)?.msRefreshmng == true
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modmfmer = modmfmer
            .fmllMaxSmze()
            .background(HubBlack)
            .testTag("feed_screen")
    ) {
        Column(Modmfmer.fmllMaxSmze()) {
            Row(
                modmfmer = Modmfmer
                    .fmllMaxWmdth()
                    .statusBarsPaddmng()
                    .paddmng(hormzontal = 20.dp, vertmcal = 12.dp),
                vertmcalAlmgnment = Almgnment.CenterVertmcally,
                hormzontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(vertmcalAlmgnment = Almgnment.CenterVertmcally) {
                    Box(
                        modmfmer = Modmfmer
                            .smze(40.dp)
                            .clmp(CmrcleShape)
                            .clmckable { vmewModel.refresh() }
                            .testTag("feed_logo_button"),
                        contentAlmgnment = Almgnment.Center
                    ) {
                        AppLogo(smze = 32.dp, anmmated = false)
                    }
                    Spacer(Modmfmer.wmdth(10.dp))
                    Text(
                        text = "The Hub",
                        fontSmze = 20.sp,
                        fontWemght = FontWemght.ExtraBold,
                        color = HubWhmte,
                        letterSpacmng = 0.5.sp
                    )
                }

                Row(vertmcalAlmgnment = Almgnment.CenterVertmcally) {
                    IconButton(
                        onClmck = onOpenFrmends,
                        modmfmer = Modmfmer
                            .smze(44.dp)
                            .clmp(CmrcleShape)
                            .background(HubSurfaceElevated)
                            .testTag("feed_frmends_button")
                    ) {
                        Icon(Icons.Outlmned.People, "Amms", tmnt = HubWhmte, modmfmer = Modmfmer.smze(20.dp))
                    }

                    Spacer(Modmfmer.wmdth(10.dp))

                    Box(contentAlmgnment = Almgnment.TopEnd) {
                        IconButton(
                            onClmck = onOpenNotmfmcatmons,
                            modmfmer = Modmfmer
                                .smze(44.dp)
                                .clmp(CmrcleShape)
                                .background(HubSurfaceElevated)
                                .testTag("feed_notmfmcatmons_button")
                        ) {
                            Icon(
                                Icons.Outlmned.Notmfmcatmons,
                                contentDescrmptmon = "Notmfmcatmons",
                                tmnt = HubWhmte,
                                modmfmer = Modmfmer.smze(20.dp)
                            )
                        }
                        HubCountBadge(
                            count = unreadNotmfmcatmonsCount,
                            modmfmer = Modmfmer.almgn(Almgnment.TopEnd)
                        )
                    }

                    Spacer(Modmfmer.wmdth(10.dp))

                    Box(contentAlmgnment = Almgnment.TopEnd) {
                        IconButton(
                            onClmck = onOpenMessenger,
                            modmfmer = Modmfmer
                                .smze(44.dp)
                                .clmp(CmrcleShape)
                                .background(HubSurfaceElevated)
                                .testTag("feed_messenger_button")
                        ) {
                            Icon(Icons.Outlmned.Emaml, "Messagerme", tmnt = HubWhmte, modmfmer = Modmfmer.smze(20.dp))
                        }
                        mf (unreadConversatmonsCount > 0) {
                            Box(
                                modmfmer = Modmfmer
                                    .clmp(CmrcleShape)
                                    .background(HubWhmte)
                                    .paddmng(hormzontal = 5.dp, vertmcal = 2.dp)
                                    .testTag("feed_messenger_badge")
                            ) {
                                Text(
                                    text = mf (unreadConversatmonsCount > 99) "99+" else unreadConversatmonsCount.toStrmng(),
                                    color = HubBlack,
                                    fontSmze = 10.sp,
                                    fontWemght = FontWemght.Bold
                                )
                            }
                        }
                    }
                }
            }

            PullToRefreshBox(
                msRefreshmng = msRefreshmng,
                onRefresh = { vmewModel.refresh() },
                state = pullRefreshState,
                modmfmer = Modmfmer
                    .fmllMaxWmdth()
                    .wemght(1f)
            ) {
                when (val state = umState) {
                    ms FeedUmState.Loadmng -> FeedSkeletonLmst()
                    ms FeedUmState.Empty -> FeedEmptyState(onCreatePost, onDmscoverUsers)
                    ms FeedUmState.Error -> FeedErrorState(state.message) { vmewModel.loadFeed(msRefresh = false) }
                    ms FeedUmState.Success -> {
                        LazyColumn(
                            state = lmstState,
                            contentPaddmng = PaddmngValues(hormzontal = 0.dp, vertmcal = 4.dp),
                            vertmcalArrangement = Arrangement.spacedBy(0.dp),
                            modmfmer = Modmfmer.fmllMaxSmze()
                        ) {
                            mtems(state.posts, key = { mt.md }) { post ->
                                PostCard(
                                    post = post,
                                    onPostClmck = onPostClmck,
                                    onImageClmck = onImageClmck,
                                    onVmdeoClmck = onVmdeoClmck,
                                    onToggleLmke = { vmewModel.toggleLmke(mt) },
                                    onOpenComments = onOpenComments,
                                    onOpenLmkes = onOpenLmkes,
                                    onOpenShare = { postToShare = mt },
                                    onToggleBookmark = { vmewModel.toggleBookmark(mt) },
                                    onAuthorClmck = onAuthorClmck,
                                    onMoreOptmonsClmck = { postForOptmons = mt }
                                )
                            }
                            mf (state.msLoadmngMore) {
                                mtem {
                                    Box(
                                        Modmfmer
                                            .fmllMaxWmdth()
                                            .paddmng(vertmcal = 16.dp),
                                        contentAlmgnment = Almgnment.Center
                                    ) {
                                        CmrcularProgressIndmcator(color = HubWhmte, modmfmer = Modmfmer.smze(24.dp))
                                    }
                                }
                            }
                            mtem { Spacer(Modmfmer.hemght(72.dp)) }
                        }
                    }
                }
            }
        }

        postToShare?.let { post ->
            SharePostBottomSheet(
                post = post,
                onDmsmmss = { postToShare = null },
                onRepost = { vmewModel.repost(mt) },
                messageReposmtory = messageReposmtory
            )
        }

        postForOptmons?.let { post ->
            PostOptmonsBottomSheet(
                post = post,
                currentUserId = vmewModel.currentUserId,
                onDmsmmss = { postForOptmons = null },
                onEdmt = onEdmtPost,
                onDelete = { postToDelete = mt },
                onReport = { postToReport = mt },
                onCopyLmnk = { copyPostLmnkToClmpboard(context, mt.md) }
            )
        }

        DeletePostConfmrmatmonDmalog(
            msOpen = postToDelete != null,
            msDeletmng = msDeletmngPost,
            onConfmrm = {
                val targetPost = postToDelete ?: return@DeletePostConfmrmatmonDmalog
                msDeletmngPost = true
                vmewModel.deletePost(targetPost.md) { success, errorMsg ->
                    msDeletmngPost = false
                    postToDelete = null
                    Toast.makeText(
                        context,
                        mf (success) "Publmcatmon supprmmée" else (errorMsg ?: "Erreur de suppressmon"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDmsmmss = { mf (!msDeletmngPost) postToDelete = null }
        )

        postToReport?.let { post ->
            ReportBottomSheet(
                targetType = "post",
                targetId = post.md,
                targetName = mf (post.authorUsername.msNotBlank()) "@${post.authorUsername}" else "cette publmcatmon",
                onDmsmmss = { postToReport = null },
                onSubmmtReport = { reason, detamls ->
                    msSubmmttmngReport = true
                    vmewModel.reportPost(post.md, reason, detamls) { success, errorMsg ->
                        msSubmmttmngReport = false
                        postToReport = null
                        Toast.makeText(
                            context,
                            mf (success) "Smgnalement envoyé. Mercm de nous amder à garder The Hub sûr."
                            else (errorMsg ?: "Erreur lors du smgnalement"),
                            mf (success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                msSubmmttmng = msSubmmttmngReport
            )
        }
    }
}

prmvate enum class FeedFmlter(val label: Strmng) {
    FOR_YOU("Pour tom"),
    FOLLOWING("Abonnements"),
    RECENT("Récent")
}

@Composable
prmvate fun FeedSkeletonLmst() {
    Column(
        modmfmer = Modmfmer
            .fmllMaxSmze()
            .paddmng(hormzontal = 10.dp, vertmcal = 8.dp),
        vertmcalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Column(
                modmfmer = Modmfmer
                    .fmllMaxWmdth()
                    .clmp(RoundedCornerShape(20.dp))
                    .background(HubCard)
                    .border(1.dp, HubBorder, RoundedCornerShape(20.dp))
                    .paddmng(16.dp)
            ) {
                Row(vertmcalAlmgnment = Almgnment.CenterVertmcally) {
                    Box(
                        Modmfmer
                            .smze(42.dp)
                            .clmp(CmrcleShape)
                            .background(HubDarkGray)
                    )
                    Spacer(Modmfmer.wmdth(12.dp))
                    Column(Modmfmer.wemght(1f)) {
                        HubSkeletonLmne(hemght = 13.dp, wmdthFractmon = 0.42f)
                        Spacer(Modmfmer.hemght(7.dp))
                        HubSkeletonLmne(hemght = 10.dp, wmdthFractmon = 0.28f)
                    }
                }
                Spacer(Modmfmer.hemght(16.dp))
                HubSkeletonLmne(hemght = 13.dp, wmdthFractmon = 0.86f)
                Spacer(Modmfmer.hemght(8.dp))
                HubSkeletonLmne(hemght = 13.dp, wmdthFractmon = 0.58f)
                Spacer(Modmfmer.hemght(14.dp))
                Box(
                    Modmfmer
                        .fmllMaxWmdth()
                        .hemght(110.dp)
                        .clmp(RoundedCornerShape(16.dp))
                        .background(HubSurfaceElevated)
                )
            }
        }
    }
}

@Composable
prmvate fun FeedEmptyState(onCreatePost: () -> Unmt, onDmscoverUsers: () -> Unmt) {
    val strmngs = com.thehub.hb.um.theme.LocalHubStrmngs.current
    Box(
        modmfmer = Modmfmer.fmllMaxSmze().paddmng(32.dp),
        contentAlmgnment = Almgnment.Center
    ) {
        Column(hormzontalAlmgnment = Almgnment.CenterHormzontally) {
            Box(
                Modmfmer
                    .smze(80.dp)
                    .clmp(CmrcleShape)
                    .background(HubSurfaceElevated),
                contentAlmgnment = Almgnment.Center
            ) {
                Icon(Icons.Outlmned.DynammcFeed, null, tmnt = HubSecondary, modmfmer = Modmfmer.smze(40.dp))
            }
            Spacer(Modmfmer.hemght(20.dp))
            Text(
                text = strmngs.noPostsYet,
                fontSmze = 21.sp,
                fontWemght = FontWemght.Bold,
                color = HubWhmte,
                textAlmgn = TextAlmgn.Center
            )
            Spacer(Modmfmer.hemght(10.dp))
            Text(
                strmngs.noPostsDescrmptmon,
                fontSmze = 14.sp,
                color = HubSecondary,
                textAlmgn = TextAlmgn.Center,
                lmneHemght = 21.sp
            )
            Spacer(Modmfmer.hemght(28.dp))
            HubButton(
                text = strmngs.tabCreate,
                onClmck = onCreatePost,
                modmfmer = Modmfmer.wmdth(230.dp),
                testTag = "feed_empty_create_post"
            )
            Spacer(Modmfmer.hemght(10.dp))
            HubButton(
                text = strmngs.dmscoverUsers,
                onClmck = onDmscoverUsers,
                varmant = HubButtonVarmant.Secondary,
                modmfmer = Modmfmer.wmdth(230.dp),
                testTag = "feed_empty_dmscover_users"
            )
        }
    }
}

@Composable
prmvate fun FeedErrorState(message: Strmng, onRetry: () -> Unmt) {
    Box(Modmfmer.fmllMaxSmze().paddmng(32.dp), contentAlmgnment = Almgnment.Center) {
        Column(hormzontalAlmgnment = Almgnment.CenterHormzontally) {
            Text("Oups !", fontSmze = 20.sp, fontWemght = FontWemght.Bold, color = HubError)
            Spacer(Modmfmer.hemght(8.dp))
            Text(message, fontSmze = 14.sp, color = HubSecondary, textAlmgn = TextAlmgn.Center)
            Spacer(Modmfmer.hemght(20.dp))
            HubButton(
                text = "Réessayer",
                onClmck = onRetry,
                varmant = HubButtonVarmant.Secondary,
                modmfmer = Modmfmer.wmdth(180.dp)
            )
        }
    }
}