// src/composables/useDashboard.js
import { ref, computed } from 'vue'
import api from '@/api/config'

/**
 * ✅ SIMPLIFIED Composable for Dashboard Data Management
 *
 * Features:
 * - Fetch complete dashboard data from /api/users/me/dashboard
 * - Cache management (5 minutes)
 * - Loading states
 * - Error handling
 *
 * Data Structure:
 * - user: UserDetailDto (User + Stats + Activity)
 * - quickStats: { currentStreak, totalPoints, totalLessonsCompleted, studyTimeToday, weeklyGoalProgress }
 * - skillProgress: { grammar: {...}, reading: {...}, listening: {...} }
 * - streak: { currentStreak, longestStreak, lastStreakDate, hasStreakToday, ... }
 */
export function useDashboard() {
  const dashboardData = ref(null)
  const isLoading = ref(false)
  const error = ref(null)
  const lastFetchTime = ref(null)

  // Cache duration: 5 minutes
  const CACHE_DURATION = 5 * 60 * 1000

  /**
   * Fetch complete dashboard data
   */
  const fetchDashboard = async (force = false) => {
    // Check cache
    if (!force && dashboardData.value && lastFetchTime.value) {
      const timeSinceLastFetch = Date.now() - lastFetchTime.value
      if (timeSinceLastFetch < CACHE_DURATION) {
        console.log('✅ Using cached dashboard data')
        return dashboardData.value
      }
    }

    isLoading.value = true
    error.value = null

    try {
      const response = await api.get('/api/users/me/dashboard')

      if (response.data.success) {
        dashboardData.value = response.data.data
        lastFetchTime.value = Date.now()
        console.log('✅ Dashboard data fetched:', dashboardData.value)
        return dashboardData.value
      } else {
        throw new Error(response.data.message || 'Failed to fetch dashboard')
      }
    } catch (err) {
      console.error('❌ Error fetching dashboard:', err)
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Refresh dashboard (force fetch)
   */
  const refreshDashboard = async () => {
    return await fetchDashboard(true)
  }

  /**
   * Clear cache
   */
  const clearCache = () => {
    dashboardData.value = null
    lastFetchTime.value = null
    error.value = null
  }

  // =========================================================================
  // COMPUTED PROPERTIES - Extract specific data
  // =========================================================================

  const user = computed(() => dashboardData.value?.user || null)
  const quickStats = computed(() => dashboardData.value?.quickStats || {})
  const skillProgress = computed(() => dashboardData.value?.skillProgress || {})
  const streak = computed(() => dashboardData.value?.streak || {})

  // =========================================================================
  // HELPER COMPUTED
  // =========================================================================

  const hasData = computed(() => dashboardData.value !== null)

  const isStreakActive = computed(() => streak.value?.hasStreakToday || false)

  const totalLessonsCompleted = computed(() => {
    const skills = skillProgress.value
    return (
      (skills.grammar?.completed || 0) +
      (skills.reading?.completed || 0) +
      (skills.listening?.completed || 0)
    )
  })

  const hasAnyProgress = computed(() => totalLessonsCompleted.value > 0)

  return {
    // State
    dashboardData,
    isLoading,
    error,

    // Methods
    fetchDashboard,
    refreshDashboard,
    clearCache,

    // Extracted data
    user,
    quickStats,
    skillProgress,
    streak,

    // Helper computed
    hasData,
    isStreakActive,
    totalLessonsCompleted,
    hasAnyProgress,
  }
}
