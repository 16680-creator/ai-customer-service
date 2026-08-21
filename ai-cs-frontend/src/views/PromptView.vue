<template>
  <div class="prompt-page">
    <el-card shadow="hover">
      <template #header>
        <div class="header">
          <span class="title">Prompt 配置管理</span>
          <el-tag size="small" type="info" effect="plain">配置化版本管理 · 热切换无需重启</el-tag>
        </div>
      </template>

      <el-row :gutter="16">
        <!-- 左：场景列表 -->
        <el-col :span="8">
          <el-input v-model="filter" placeholder="筛选场景" clearable size="small" style="margin-bottom: 12px" />
          <el-menu :default-active="activeScenario" class="scenario-menu" @select="selectScenario">
            <el-menu-item v-for="s in filteredScenarios" :key="s.scenario" :index="s.scenario">
              <div class="scenario-item">
                <span>{{ s.scenario }}</span>
                <el-tag v-if="s.activeVersion" size="small" type="success" effect="plain">v{{ s.activeVersion }}</el-tag>
              </div>
            </el-menu-item>
            <el-empty v-if="filteredScenarios.length === 0" description="暂无场景" :image-size="80" />
          </el-menu>
        </el-col>

        <!-- 右：版本列表 + 内容预览 -->
        <el-col :span="16">
          <div v-if="!activeScenario" class="placeholder">
            <el-empty description="选择左侧场景查看 Prompt 版本" />
          </div>
          <template v-else>
            <div class="version-header">
              <span class="scenario-name">{{ activeScenario }}</span>
              <el-button size="small" :icon="Refresh" :loading="loadingVersions" @click="loadVersions">刷新</el-button>
            </div>

            <el-table :data="versions" stripe size="small" border>
              <el-table-column prop="version" label="版本" width="90" />
              <el-table-column label="生效" width="80">
                <template #default="{ row }">
                  <el-tag v-if="row.version === activeVersion" size="small" type="success">生效中</el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="内容长度" width="100">
                <template #default="{ row }">{{ row.contentLength ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="row.version === activeVersion"
                    @click="setActive(row.version)"
                  >设为生效</el-button>
                  <el-button size="small" text type="info" @click="openVersionPreview(row)">预览</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-dialog v-model="showPreview" :title="`${activeScenario} · 版本 ${previewVersion}`" width="720px">
              <pre class="prompt-content">{{ previewContent || '（无内容）' }}</pre>
            </el-dialog>
          </template>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { promptApiWrappers } from '../api'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const scenarios = ref([])
const filter = ref('')
const activeScenario = ref('')
const versions = ref([])
const activeVersion = ref(null)
const loadingVersions = ref(false)
const showPreview = ref(false)
const previewVersion = ref('')
const previewContent = ref('')

const filteredScenarios = computed(() => {
  const f = filter.value.trim().toLowerCase()
  if (!f) return scenarios.value
  return scenarios.value.filter(s => s.scenario.toLowerCase().includes(f))
})

onMounted(loadScenarios)

async function loadScenarios() {
  try {
    const resp = await promptApiWrappers.list()
    const data = resp?.data?.data ?? resp?.data ?? []
    scenarios.value = Array.isArray(data) ? data : []
    if (scenarios.value.length && !activeScenario.value) {
      selectScenario(scenarios.value[0].scenario)
    }
  } catch (e) {
    ElMessage.error('加载 Prompt 场景失败: ' + (e.message || ''))
  }
}

async function selectScenario(scenario) {
  activeScenario.value = scenario
  await loadVersions()
}

async function loadVersions() {
  if (!activeScenario.value) return
  loadingVersions.value = true
  try {
    const resp = await promptApiWrappers.listVersions(activeScenario.value)
    const data = resp?.data?.data ?? resp?.data ?? {}
    versions.value = data.versions || []
    activeVersion.value = data.activeVersion ?? null
  } catch (e) {
    ElMessage.error('加载版本失败: ' + (e.message || ''))
  } finally {
    loadingVersions.value = false
  }
}

async function setActive(version) {
  try {
    await promptApiWrappers.setActive(activeScenario.value, version)
    ElMessage.success(`已将 ${activeScenario.value} 切换至 v${version}`)
    await loadVersions()
  } catch (e) {
    ElMessage.error('切换失败: ' + (e.message || ''))
  }
}

async function openVersionPreview(row) {
  previewVersion.value = row.version
  previewContent.value = row.content || ''
  showPreview.value = true
}
</script>

<style scoped>
.prompt-page { padding: 16px; }
.header { display: flex; align-items: center; gap: 12px; }
.title { font-weight: 600; color: #303133; }
.scenario-menu { border-right: none; }
.scenario-item { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.version-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.scenario-name { font-weight: 600; font-size: 15px; color: #303133; }
.prompt-content { background: #f5f7fa; padding: 14px; border-radius: 6px; font-size: 13px; line-height: 1.6; white-space: pre-wrap; max-height: 480px; overflow: auto; }
.placeholder { min-height: 240px; }
</style>
