<!--	
	@component
	A component to edit DUUIRegistry that appears on the right side on the screen (Sidebar Drawer).
-->
<script lang="ts">
	import { errorToast, successToast } from '$lib/duui/utils/ui'
	import {
		registryStore,
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
	import Dropdown from '../Input/Dropdown.svelte'
	import Popup from '../Popup.svelte'
	import JsonDropdownInput from '../Input/JsonDropdownInput.svelte'

	const drawerStore = getDrawerStore()
	
	
	let registryId: string = $drawerStore.meta.registryId
	let currentRegistry: DUUIRegistry = $drawerStore.meta.registry
	let registryScopes = GlobalScopes

	let groups = $groupStore as DUUIGroups
	let selectedGroups = currentRegistry.groups || []

	let selectedGroupsMap: Map<string, string> = new Map(
		selectedGroups
			.map((groupId) => [groupId, groups[groupId].name])
	)
	let groupsMap: Map<string, string> = new Map(
		Object.entries(groups).map(([id, details]) => [id, details.name])
	)


	const isNewRegistry: boolean = $drawerStore.meta.creating

	const modalStore = getModalStore()
	const toastStore = getToastStore()


	const onUpdate = async () => {

		currentRegistry.groups = Array.from(selectedGroupsMap.keys())

		const response = await fetch('/api/users/registry', {
			method: 'PUT',
			body: JSON.stringify({
				registryId: registryId,
				registry: currentRegistry
			})
		})

		if (response.ok) {
			if ($registryStore) {
				$registryStore[registryId] = currentRegistry
				$registryStore = $registryStore
			}
			toastStore.trigger(successToast('Registry updated successfully'))
		} else {
			toastStore.trigger(errorToast('Error: ' + response.statusText))
		}

		drawerStore.close()
	}

	const onDelete = async () => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Registry',
				message: `Are you sure you want to delete ${currentRegistry.name}?`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return


		if ($registryStore) {
			delete $registryStore[registryId]
			$registryStore = $registryStore
		}

		const response = await fetch('/api/users/registry', {
			method: 'DELETE',
			body: JSON.stringify({
				registryId: registryId
			})
		})
		if (response.ok) {
			toastStore.trigger(successToast('Registry deleted successfully'))

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
			disabled={!currentRegistry.name}
	>
		<Fa icon={faFileCircleCheck} />
		Save
		{#if !currentRegistry.name}
			<Tip> A name is required to save. </Tip>
		{/if}
	</button>
	{#if isNewRegistry}
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
			<h3 class="h3">Registry Editor</h3>
		</div>

		<div class="hidden md:flex space-x-2">
			<button class="button-neutral" on:click={drawerStore.close}>
				<Fa icon={faAngleDoubleRight} size="lg" />
				<span>Close</span>
			</button>

			<Popup autoPopupWidth={true}>
				<svelte:fragment slot="trigger">
					<button class="button-neutral" on:click={() => onUpdate()} disabled={!currentRegistry.name}>
						<Fa icon={faFileCircleCheck} />
						Save
					</button>
				</svelte:fragment>
				<svelte:fragment slot="popup">
					{#if !currentRegistry.name}
						<Tip customIcon={faWarning} tipTheme="tertiary"> A name is required to save. </Tip>
					{/if}
				</svelte:fragment>
			</Popup>

			{#if isNewRegistry}
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
			<TextInput label="Name" name="name" bind:value={currentRegistry.name} />

			<TextInput label="Endpoint" name="endpoint" bind:value={currentRegistry.endpoint} />
		</div>

		<div class="space-y-2 ">
			<Dropdown label="Scope" name="driver" options={registryScopes} bind:value={currentRegistry.scope} />
		</div>

		{#if currentRegistry.scope === "GROUP"}
			<div class="space-y-2">
				<h4 class="h4">Groups</h4>
				<JsonDropdownInput bind:dropdownList={groupsMap} bind:data={selectedGroupsMap} leadingIcon={faPeopleGroup}/>
			</div>

		{/if}
	</div>
</div>