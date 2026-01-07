// mixins/roleMixin.js - SHARED ROLE LOGIC
import { useAuthStore } from '@/stores/auth'

/**
 * ✅ Role-based Mixin for Vue Components
 * Use in Grammar/Reading/Listening Management Views
 */
export const roleMixin = {
  computed: {
    authStore() {
      return useAuthStore()
    },

    // ✅ Current role (ADMIN | TEACHER | USER)
    currentRole() {
      return this.authStore.currentRole
    },

    // ✅ Check if admin
    isAdmin() {
      return this.currentRole === 'ADMIN'
    },

    // ✅ Check if teacher
    isTeacher() {
      return this.currentRole === 'TEACHER'
    },

    // ✅ Check if user
    isUser() {
      return this.currentRole === 'USER'
    },

    // ✅ Check if management role (Admin or Teacher)
    isManagement() {
      return this.isAdmin || this.isTeacher
    },

    // ✅ Can create/edit/delete (both Admin and Teacher can)
    canCreate() {
      return this.isManagement
    },

    canEdit() {
      return this.isManagement
    },

    canDelete() {
      return this.isManagement
    },

    // ✅ Can manage topics (Admin only)
    canManageTopics() {
      return this.isAdmin
    },

    // ✅ Can manage users (Admin only)
    canManageUsers() {
      return this.isAdmin
    },

    // ✅ Can assign teachers (Admin only)
    canAssignTeachers() {
      return this.isAdmin
    },

    // ✅ Portal base path
    portalPath() {
      if (this.isAdmin) return '/admin'
      if (this.isTeacher) return '/teacher'
      return '/user'
    },
  },

  methods: {
    // ✅ Show role-specific message
    showRoleMessage(message) {
      const rolePrefix = this.isAdmin ? '[Admin]' : this.isTeacher ? '[Teacher]' : '[User]'
      console.log(`${rolePrefix} ${message}`)
    },

    // ✅ Handle permission denied
    handlePermissionDenied(action = 'thực hiện hành động này') {
      this.$toast.error(`Bạn không có quyền ${action}`)
    },

    // ✅ Check if can perform action
    checkPermission(action, showToast = true) {
      const permissions = {
        create: this.canCreate,
        edit: this.canEdit,
        delete: this.canDelete,
        manageTopics: this.canManageTopics,
        manageUsers: this.canManageUsers,
        assignTeachers: this.canAssignTeachers,
      }

      const hasPermission = permissions[action]

      if (!hasPermission && showToast) {
        this.handlePermissionDenied(action)
      }

      return hasPermission
    },
  },
}

export default roleMixin
