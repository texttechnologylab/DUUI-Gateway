import type { DUUIPipeline } from '$lib/duui/pipeline'
import { pipelineToJson } from '$lib/duui/pipeline'
import type { ModalSettings, ModalStore, ToastStore } from '@skeletonlabs/skeleton'
import { errorToast, infoToast } from '$lib/duui/utils/ui'
import { showConfirmationModal } from '$lib/svelte/utils/modal'

type GotoFn = (href: string) => Promise<void>

export const copyPipelineWithPrompt = async (
	pipeline: DUUIPipeline,
	modalStore: ModalStore,
	toastStore: ToastStore,
	gotoFn: GotoFn
) => {
	const newName = await new Promise<string>((resolve) => {
		const modal: ModalSettings = {
			type: 'component',
			component: 'promptModal',
			meta: {
				title: 'Copy Pipeline',
				message: 'Choose a new Name',
				value: pipeline.name + ' - Copy'
			},
			response: (r: string) => {
				resolve(r as string)
			}
		}
		modalStore.trigger(modal)
	})

	if (!newName) return

	const response = await fetch('/api/pipelines', {
		method: 'POST',
		body: JSON.stringify({
			...pipeline,
			name: newName
		})
	})

	if (response.ok) {
		const data = await response.json()
		toastStore.trigger(infoToast('Pipeline copied successfully'))
		await gotoFn(`/pipelines?id=${data.oid}`)
	} else {
		toastStore.trigger(errorToast('Error: ' + response.statusText))
	}
}

export const exportPipelineToFile = (pipeline: DUUIPipeline) => {
	const blob = new Blob([JSON.stringify(pipelineToJson(pipeline))], {
		type: 'application/json'
	})
	const url = URL.createObjectURL(blob)
	const anchor = document.createElement('a')
	anchor.href = url
	anchor.download = `${pipeline.name}.json`
	document.body.appendChild(anchor)
	anchor.click()
	document.body.removeChild(anchor)
	URL.revokeObjectURL(url)
}

export const deletePipelineWithConfirm = async (
	pipeline: DUUIPipeline,
	modalStore: ModalStore,
	toastStore: ToastStore,
	gotoFn?: GotoFn
) => {
	const confirm = await showConfirmationModal(
		{
			title: 'Delete Pipeline',
			message: `Are you sure you want to delete ${pipeline.name}?`,
			textYes: 'Delete'
		},
		modalStore
	)

	if (!confirm) return false

	const response = await fetch(`/api/pipelines`, {
		method: 'DELETE',
		body: JSON.stringify({ oid: pipeline.oid })
	})

	if (response.ok) {
		toastStore.trigger(infoToast('Pipeline deleted successfully'))

		if (gotoFn) {
			await gotoFn('/pipelines')
		}

		return true
	} else {
		toastStore.trigger(errorToast('Error: ' + response.statusText))
		return false
	}
}

