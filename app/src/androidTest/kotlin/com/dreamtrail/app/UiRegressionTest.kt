package com.dreamtrail.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ActionOutcome
import com.dreamtrail.core.model.*
import com.dreamtrail.core.save.SaveBackupCodec
import com.dreamtrail.core.save.SaveBackupPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on an isolated test emulator, including at large system font sizes. */
@RunWith(AndroidJUnit4::class)
class UiRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val repository get() = (compose.activity.application as DreamTrailApp).repository
    private val state get() = runBlocking {
        withTimeout(10_000) { repository.stateFlow.first { it.initialized } }
    }
    private val weaponDef get() = ContentCatalog.equipmentDefs.first { it.slot == EquipmentSlot.WEAPON }

    private fun seed() {
        val weapon = EquipmentInstance("ui_weapon", weaponDef.id, weaponDef.tier, 3, weaponDef.defaultAffix)
        val payload = SaveBackupPayload(
            exportedAtUtcMs = System.currentTimeMillis(),
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            playerState = PlayerState(
                level = 20,
                resources = ResourceBag(10_000L, RegionId.entries.associate { it.materialId to 1_000L }, 100L),
                facilities = FacilityType.entries.associateWith { 5 },
                equippedWeaponId = weapon.instanceId
            ),
            budgetState = RewardBudgetState(),
            discoveryState = DiscoveryState(
                unlockedRouteIds = ContentCatalog.routes.map { it.id }.toSet(),
                companionChapters = ContentCatalog.companionStories.associate { it.id to it.chapters.size },
                totalCreditedSeconds = 200 * 3600L,
                onboardingCompleted = true
            ),
            inventory = listOf(weapon)
        )
        runBlocking {
            repository.ensureInitializedAndRecover()
            val result = repository.importBackupJson(SaveBackupCodec.encode(payload))
            assertTrue(result.toString(), result is ActionOutcome.Success)
        }
        compose.waitUntil(10_000) { state.playerState.level == 20 }
        compose.waitForIdle()
        compose.onNodeWithText("旅程").performClick()
    }

    private fun scrollListTo(matcher: SemanticsMatcher) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(matcher)
    }

    private fun scrollJourneyTo(text: String) = scrollListTo(hasText(text))

    private fun scrollDialogToText(text: String) {
        val target = hasText(text) and hasAnyAncestor(isDialog())
        repeat(12) {
            if (compose.onAllNodes(target, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNode(target, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                return
            }
            compose.onNode(hasScrollAction() and hasAnyAncestor(isDialog()))
                .performTouchInput { swipeUp() }
        }
        compose.onNode(target, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun tabsKeepSelectionAndScrollAfterSwitchAndActivityRecreation() {
        seed()
        compose.onNodeWithText("角色").performClick()
        scrollListTo(hasText("星图"))
        compose.onNodeWithText("星图").performScrollTo().performClick()
        val node = ContentCatalog.astralNodes.last()
        scrollListTo(hasContentDescription(node.title))
        compose.onNodeWithContentDescription(node.title).performScrollTo().performClick()
        compose.onNodeWithContentDescription(node.title).assertIsSelected().assertIsDisplayed()

        compose.onNodeWithText("营地").performClick()
        compose.onNodeWithText("角色").performClick()
        compose.onNodeWithContentDescription(node.title).assertIsSelected().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithContentDescription(node.title).assertIsSelected().assertIsDisplayed()
    }

    @Test fun equipmentDialogCanReachAndSaveBottomAction() {
        seed()
        compose.onNodeWithText("角色").performClick()
        scrollListTo(hasContentDescription(weaponDef.name, substring = true) and hasClickAction())
        compose.onAllNodes(hasContentDescription(weaponDef.name, substring = true) and hasClickAction())[0]
            .performScrollTo().performClick()
        compose.onNodeWithText("铭刻副词条", substring = true).performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            state.inventory.firstOrNull { it.instanceId == "ui_weapon" }?.secondaryAffix != null
        }
        compose.onNode(hasText("副词条铭刻完成，流派共鸣已刷新") and hasAnyAncestor(isDialog()))
            .performScrollTo().assertIsDisplayed()
        compose.onNode(hasContentDescription("关闭") and hasAnyAncestor(isDialog())).performScrollTo().performClick()
    }

    @Test fun preparationPickerCanReachLastTeaAndPersistChoice() {
        seed()
        scrollJourneyTo("热茶")
        compose.onNodeWithText("热茶").performScrollTo().performClick()
        val tea = ContentCatalog.teaBlends.last()
        scrollDialogToText(tea.name)
        compose.onNode(hasText(tea.name) and hasAnyAncestor(isDialog()), useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) { state.playerState.activeTeaBlendId == tea.id }
        val notice = "已备好「${tea.name}」"
        compose.waitUntil(10_000) { compose.onAllNodesWithText(notice).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(notice).assertIsDisplayed()
        scrollJourneyTo("热茶")
        compose.onNodeWithText("热茶").performScrollTo().performClick()
        scrollDialogToText(tea.name)
        compose.onNodeWithText("完成").assertIsDisplayed().performClick()
    }

    @Test fun departureBlackoutAndWakeSettlementCompleteWithoutDuplicateRewards() {
        seed()
        scrollJourneyTo("出发，去休息")
        compose.onNodeWithText("出发，去休息").performScrollTo().performClick()
        compose.waitUntil(10_000) { state.runningSession != null }
        compose.onNodeWithText("极暗守夜").performScrollTo().performClick()
        compose.onNodeWithText("轻触屏幕唤醒守夜面板", substring = true).performClick()
        compose.onNodeWithText("我已醒来 · 准备结束守夜").performScrollTo().performClick()
        compose.onNodeWithText("确认结束冒险并结算战报").performScrollTo().performClick()
        compose.waitUntil(10_000) { state.runningSession == null }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("已入账").fetchSemanticsNodes().isNotEmpty() }
        scrollJourneyTo("继续准备旅程")
        compose.onNodeWithText("继续准备旅程").performScrollTo().performClick()
        assertTrue(state.recentSettlements.size == 1)
    }

    @Test fun focusPomodoroTaskSoundscapeAndSettlementFlowWorks() {
        seed()
        scrollJourneyTo("白昼专注")
        compose.onNodeWithText("白昼专注").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("番茄定心").fetchSemanticsNodes().isNotEmpty()
        }
        scrollJourneyTo("专注方向与今日行囊待办")

        scrollJourneyTo("白噪音")
        compose.onNodeWithText("白噪音").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("自然声景与白噪音台") and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("雾林细雨") and hasAnyAncestor(isDialog()), useUnmergedTree = true).performScrollTo().performClick()
        compose.onNode(hasText("关闭") and hasAnyAncestor(isDialog())).performClick()

        scrollJourneyTo("开始专注 · 踏勘出发")
        compose.onNodeWithText("开始专注 · 踏勘出发").performScrollTo().performClick()
        compose.waitUntil(10_000) { state.runningSession?.sessionMode == SessionMode.FOCUS_POMODORO }

        compose.onNodeWithText("完成本轮专注 · 结算收获").performScrollTo().performClick()
        compose.onNode(hasText("确认结算专注战报") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(10_000) { state.runningSession == null }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("已入账").fetchSemanticsNodes().isNotEmpty() }
        scrollJourneyTo("继续准备旅程")
        compose.onNodeWithText("继续准备旅程").performScrollTo().performClick()
        assertTrue(state.recentSettlements.firstOrNull()?.sessionMode == SessionMode.FOCUS_POMODORO)
    }
}
