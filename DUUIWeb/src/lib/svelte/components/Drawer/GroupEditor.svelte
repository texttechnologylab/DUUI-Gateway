<!--	
	@component
	A component to edit DUUIComponents that appears on the right side on the screen (Sidebar Drawer).
-->
<script lang="ts">
	import { DUUIDrivers, type DUUIComponent, componentToJson } from '$lib/duui/component'
	import { errorToast, successToast } from '$lib/duui/utils/ui'
	import {
		currentPipelineStore,
		exampleComponent,
		examplePipelineStore,
		userSession
	} from '$lib/store'
	import { showConfirmationModal } from '$lib/svelte/utils/modal'
	import {
		faAngleDoubleRight,
		faCancel,
		faFileCircleCheck,
		faFileExport,
		faFileImport,
		faFileUpload,
		faInfo,
		faTrash
	} from '@fortawesome/free-solid-svg-icons'
	import { FileButton, getDrawerStore, getModalStore, getToastStore } from '@skeletonlabs/skeleton'
	import pkg from 'lodash'
	import { createEventDispatcher } from 'svelte'
	import Fa from 'svelte-fa'
	import { v4 as uuidv4 } from 'uuid'
	import DriverIcon from '../DriverIcon.svelte'
	import Chips from '../Input/Chips.svelte'
	import ComponentOptions from '../Input/ComponentOptions.svelte'
	import Dropdown from '../Input/Dropdown.svelte'
	import JsonInput from '../Input/JsonInput.svelte'
	import TextArea from '../Input/TextArea.svelte'
	import TextInput from '../Input/TextInput.svelte'
	import Popup from '../Popup.svelte'
	import Tip from '../Tip.svelte'
	import { faPeopleGroup } from '@fortawesome/free-solid-svg-icons'
	import JsonDropdownInput from "$lib/svelte/components/Input/JsonDropdownInput.svelte";
	const { cloneDeep } = pkg

	type Group = {
		name: string
		members: string[]
		labels: string[]
	}

	type DUUILabels = {
		[labelId: string]: {
			label: string
			driver: string
			scope: string
		}
	}

	const drawerStore = getDrawerStore()

	let groupId = $drawerStore.meta.groupId
	let group: Group = $drawerStore.meta.group
	let labels: DUUILabels = $drawerStore.meta.labels
	let selectedLabels: Map<string, string> = $drawerStore.meta.selectedLabels

	const labelsMap: Map<string, string> = new Map(
			Object.entries(labels).map(([id, details]) => [id, details.label])
	)

	const inEditor: boolean = $drawerStore.meta.inEditor
	const creating: boolean = $drawerStore.meta.creating

	const modalStore = getModalStore()
	const toastStore = getToastStore()

	let groupName: string

	const onUpdate = async () => {
		if (!inEditor) {
			component.parameters = Object.fromEntries(parameters)
			const response = await fetch('/api/components', {
				method: 'PUT',
				body: JSON.stringify(component)
			})

			if (response.ok) {
				toastStore.trigger(successToast('Component updated successfully'))
				parameters = new Map(Object.entries(component.parameters))
				chosenLabels = new Map()
				for (let l=0; l<component.options.labels.length; l++) {
					chosenLabels.set(component.options.labels[l], component.options.labels[l])
				}
				chosenLabels = chosenLabels
			}

			dispatcher('updated', { ...component })
			if (component.pipeline_id) {
				$currentPipelineStore.components.forEach((c, index) => (c.index = index))
				component.id = uuidv4()
				$currentPipelineStore.components[component.index] = component
			}
		}

		drawerStore.close()
	}

	const onCreate = async () => {
		if (example) {
			$exampleComponent = { ...component }
			$currentPipelineStore.components[component.index] = $exampleComponent
			$currentPipelineStore.components = $currentPipelineStore.components
		} else {
			component.parameters = Object.fromEntries(parameters)

			const response = await fetch('/api/components', {
				method: 'POST',
				body: JSON.stringify({
					...component,
					index: component.index || $currentPipelineStore.components.length,
					pipeline_id: $currentPipelineStore.oid
				})
			})

			if (response.ok) {
				const result: DUUIComponent = await response.json()
				$currentPipelineStore.components.splice(component.index, 0, { ...result, id: uuidv4() })
				$currentPipelineStore.components = $currentPipelineStore.components.map(
						(c: DUUIComponent) => {
							return { ...c, index: $currentPipelineStore.components.indexOf(c) }
						}
				)
			} else {
				errorToast(response.statusText)
			}
		}

		drawerStore.close()
	}

	const onEdited = async () => {
		if (creating) {
			const copy = cloneDeep(component)
			copy.id = uuidv4()
			copy.index = component.index || $currentPipelineStore.components.length
			$currentPipelineStore.components.splice(copy.index, 0, copy)
			$currentPipelineStore.components = $currentPipelineStore.components.map(
				(c: DUUIComponent) => {
					return { ...c, index: $currentPipelineStore.components.indexOf(c) }
				}
			)
		} else {
			$currentPipelineStore.components[component.index] = component
		}

		drawerStore.close()
	}

	const onDelete = async () => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Component',
				message: `Are you sure you want to delete ${component.name}?`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return

		const response = await fetch('/api/components', {
			method: 'DELETE',
			body: JSON.stringify(component)
		})
		if (response.ok) {
			toastStore.trigger(successToast('Component deleted successfully'))

			$currentPipelineStore.components = $currentPipelineStore.components.filter(
				(c) => c.id !== component.id
			)

			dispatcher('deleteComponent', { oid: component.oid })
		} else {
			toastStore.trigger(errorToast('Error: ' + response.statusText))
		}

		drawerStore.close()
	}

</script>

<div class="menu-mobile lg:!max-w-[50%]">
	<button class="button-mobile" on:click={drawerStore.close}>
		<Fa icon={faAngleDoubleRight} size="lg" />
		<span>Close</span>
	</button>

	{#if creating}
		<button
				disabled={!component.driver || !component.name || !component.target}
				class="button-mobile"
				on:click={() => (inEditor ? onEdited() : onCreate())}
		>
			<Fa icon={faFileCircleCheck} />
			Confirm
		</button>
		<button type="button" class="button-mobile" on:click={drawerStore.close}>
			<Fa icon={faCancel} />
			Cancel
		</button>
	{:else}
		<button
				class="button-mobile"
				on:click={() => (inEditor ? onEdited() : onUpdate())}
				disabled={!component.driver || !component.name || !component.target}
		>
			<Fa icon={faFileCircleCheck} />
			Save
		</button>
		<button
				type="button"
				class="button-mobile"
				on:click={() => (inEditor ? onRemove() : onDelete())}
		>
			<Fa icon={faTrash} />
			Delete
		</button>
	{/if}
</div>

<div class="bg-surface-50-900-token h-screen relative">
	<div
		class="p-4 space-y-4 justify-between sticky top-0 z-10 bg-surface-100-800-token border-b border-color"
	>
		<div class="flex-center-4">
			<div class="flex-center-4">
				<Fa icon={faPeopleGroup} size="lg" />
				<h3 class="h3">Group Editor</h3>
			</div>
		</div>
	</div>

	<div class="space-y-8 p-4 bg-surface-50-900-token pb-16 md:pb-4">
		<div class="space-y-4">
			<h4 class="h4">Properties</h4>

			<TextInput label="Group" name="group" bind:value={groupName} />

		</div>

		<div class="space-y-2 ">
			<h4 class="h4">Members</h4>
			<JsonDropdownInput bind:dropdownList={membersMap} bind:data={selectedMembers}/>
		</div>

		<div class="space-y-2 ">
			<h4 class="h4">Labels</h4>
			<JsonDropdownInput bind:dropdownList={labelsMap} bind:data={selectedLabels}/>
		</div>
	</div>
</div>
