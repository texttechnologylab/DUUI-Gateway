<!--	
	@component
	A component to edit DUUILabels that appears on the right side on the screen (Sidebar Drawer).
-->
<script lang="ts">
	import { errorToast, successToast } from '$lib/duui/utils/ui'
	import {
		labelStore,
		groupStore
	} from '$lib/store'
	import { showConfirmationModal } from '$lib/svelte/utils/modal'
	import {
		faAngleDoubleRight,
		faCancel,
		faFileCircleCheck,
		faTrash,

		faWarning

	} from '@fortawesome/free-solid-svg-icons'
	import { getDrawerStore, getModalStore, getToastStore } from '@skeletonlabs/skeleton'
	import Fa from 'svelte-fa'
	import TextInput from '../Input/TextInput.svelte'
	import Tip from '../Tip.svelte'
	import { faPeopleGroup } from '@fortawesome/free-solid-svg-icons'
	import { DUUIDriverFilters } from '$lib/duui/component'
	import Dropdown from '../Input/Dropdown.svelte'
	import Popup from '../Popup.svelte'
	import JsonDropdownInput from '../Input/JsonDropdownInput.svelte'

	const drawerStore = getDrawerStore()
	
	
	let labelId: string = $drawerStore.meta.labelId
	let currentLabel: DUUILabel = $drawerStore.meta.label
	let driverOptions = DUUIDriverFilters

	let groups = $groupStore as DUUIGroups
	let selectedGroups = currentLabel.groups || []

	let selectedGroupsMap: Map<string, string> = new Map(
		selectedGroups
			.map((groupId) => [groupId, groups[groupId].name])
	)
	let groupsMap: Map<string, string> = new Map(
		Object.entries(groups).map(([id, details]) => [id, details.name])
	)


	const isNewLabel: boolean = $drawerStore.meta.creating

	const modalStore = getModalStore()
	const toastStore = getToastStore()


	const onUpdate = async () => {

		currentLabel.groups = Array.from(selectedGroupsMap.keys())

		const response = await fetch('/api/users/labels', {
			method: 'PUT',
			body: JSON.stringify({
				labelId: labelId,
				label: currentLabel
			})
		})

		if (response.ok) {
			if ($labelStore) {
				$labelStore[labelId] = currentLabel
				$labelStore = $labelStore
			}
			toastStore.trigger(successToast('Label updated successfully'))
		} else {
			toastStore.trigger(errorToast('Error: ' + response.statusText))
		}

		drawerStore.close()
	}

	const onDelete = async () => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Component',
				message: `Are you sure you want to delete ${currentLabel.label}?`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return


		if ($labelStore) {
			delete $labelStore[labelId]
			$labelStore = $labelStore
		}

		const response = await fetch('/api/users/labels', {
			method: 'DELETE',
			body: JSON.stringify({
				labelId: labelId
			})
		})
		if (response.ok) {
			toastStore.trigger(successToast('Label deleted successfully'))

			// dispatcher('deleteComponent', { oid: component.oid })
		} else {
			toastStore.trigger(errorToast('Error: ' + response.statusText))
		}

		drawerStore.close()
	}

</script>


<div class="menu-mobile float-right lg:!max-w-[50%]">
	<button class="button-mobile" on:click={drawerStore.close}>
		<Fa icon={faAngleDoubleRight} size="lg" />
		<span>Close</span>
	</button>

	<button class="button-mobile" on:click={() => onUpdate()}
			disabled={!currentLabel.label}
	>
		<Fa icon={faFileCircleCheck} />
		Save
		{#if !currentLabel.label}
			<Tip> A label name is required to save. </Tip>
		{/if}
	</button>
	{#if isNewLabel}
		<button type="button" class="button-mobile" on:click={drawerStore.close}>
			<Fa icon={faCancel} />
			Cancel
		</button>
	{:else}
		<button type="button" class="button-mobile" on:click={() => (onDelete())}>
			<Fa icon={faTrash} />
			Delete
		</button>
	{/if}
</div>

<div class="bg-surface-50-900-token h-screen relative">
	
	<div class="p-4 flex justify-between items-center sticky top-0 z-10 bg-surface-100-800-token border-b border-color">
		<div class="flex-center-4">
			<Fa icon={faPeopleGroup} size="lg" />
			<h3 class="h3">Label Editor</h3>
		</div>

		<div class="hidden md:flex space-x-2">
			<button class="button-neutral" on:click={drawerStore.close}>
				<Fa icon={faAngleDoubleRight} size="lg" />
				<span>Close</span>
			</button>

			<Popup autoPopupWidth={true}>
				<svelte:fragment slot="trigger">
					<button class="button-neutral" on:click={() => onUpdate()} disabled={!currentLabel.label}>
						<Fa icon={faFileCircleCheck} />
						Save
					</button>
				</svelte:fragment>
				<svelte:fragment slot="popup">
					{#if !currentLabel.label}
						<Tip customIcon={faWarning} tipTheme="tertiary"> A label name is required to save. </Tip>
					{/if}
				</svelte:fragment>
			</Popup>

			{#if isNewLabel}
				<button type="button" class="button-neutral" on:click={drawerStore.close}>
					<Fa icon={faCancel} />
					Cancel
				</button>
			{:else}
				<button type="button" class="button-neutral" on:click={() => (onDelete())}>
					<Fa icon={faTrash} />
					Delete
				</button>
			{/if}
		</div>
	</div>

	<div class="space-y-8 p-4 bg-surface-50-900-token pb-16 md:pb-4">
		<div class="space-y-4">
			<h4 class="h4">Properties</h4>

			<TextInput label="Name" name="name" bind:value={currentLabel.label} />

		</div>

		<div class="space-y-2 ">
			<Dropdown label="Driver" name="driver" options={driverOptions} bind:value={currentLabel.driver} />
		</div>

		<div class="space-y-2">
			<h4 class="h4">Groups</h4>
			<JsonDropdownInput bind:dropdownList={groupsMap} bind:data={selectedGroupsMap} leadingIcon={faPeopleGroup}/>
		</div>
	</div>
</div>