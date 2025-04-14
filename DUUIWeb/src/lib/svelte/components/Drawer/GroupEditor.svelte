<!--	
	@component
	A component to edit DUUIComponents that appears on the right side on the screen (Sidebar Drawer).
-->
<script lang="ts">
	import { errorToast, successToast } from '$lib/duui/utils/ui'
	import {
		groupStore,
	} from '$lib/store'
	import { showConfirmationModal } from '$lib/svelte/utils/modal'
	import {
		faAngleDoubleRight,
		faCancel,
		faFileCircleCheck,
		faGear,
		faPerson,
		faTrash,

		faWarning

	} from '@fortawesome/free-solid-svg-icons'
	import { getDrawerStore, getModalStore, getToastStore } from '@skeletonlabs/skeleton'
	import Fa from 'svelte-fa'
	import TextInput from '../Input/TextInput.svelte'
	import Tip from '../Tip.svelte'
	import { faPeopleGroup } from '@fortawesome/free-solid-svg-icons'
	import JsonDropdownInput from "$lib/svelte/components/Input/JsonDropdownInput.svelte";
	import Popup from '../Popup.svelte'
	
	const drawerStore = getDrawerStore()
	
	let groupId: string = $drawerStore.meta.groupId
	let group: DUUIGroup = $drawerStore.meta.group

	let users: UserProperties[] = $drawerStore.meta.users
	let selectedMembers = group.members

	const findUser: (uid: string) => UserProperties | undefined = (userId) => users.find((user) => user.oid === userId);
	
	let selectedMembersMap: Map<string, string> = new Map(
		selectedMembers
		.map((userId) => [userId, findUser(userId)?.email ?? ""])
	)

	let membersMap: Map<string, string> = new Map(
		users
			// .filter((users) => users.email !== undefined)
			.map((member) => [member.oid, member.email as string])
	)

	const isNewGroup: boolean = $drawerStore.meta.creating

	const modalStore = getModalStore()
	const toastStore = getToastStore()


	const onUpdate = async () => {

		group.members = Array.from(selectedMembersMap.keys())
		
		const response = await fetch('/api/users/groups', {
			method: 'PUT',
			body: JSON.stringify({
				groupId: groupId,
				group: group
			})
		})

		if (response.ok) {
			if ($groupStore && $groupStore[groupId]) {
				$groupStore[groupId] = group
				$groupStore = $groupStore
			}
			toastStore.trigger(successToast('Group updated successfully'))
		} else {
			toastStore.trigger(errorToast('Error: ' + response.statusText))
		}

		drawerStore.close()
	}

	const onDelete = async () => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Component',
				message: `Are you sure you want to delete ${group.name}?`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return
		
		if ($groupStore) {
			delete $groupStore[groupId]
			$groupStore = $groupStore
		}

		const response = await fetch('/api/users/groups', {
			method: 'DELETE',
			body: JSON.stringify({
				groupId: groupId
			})
		})
		if (response.ok) {
				toastStore.trigger(successToast('Group deleted successfully'))	
			
			// dispatcher('deleteComponent', { oid: component.oid })
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

	<Popup autoPopupWidth={true}>
		<svelte:fragment slot="trigger">
			<button class="button-mobile" on:click={() => onUpdate()} disabled={!group.name}>
				<Fa icon={faFileCircleCheck} />
				Save
			</button>
		</svelte:fragment>
		<svelte:fragment slot="popup">
			{#if !group.name}
				<Tip customIcon={faWarning} tipTheme="tertiary"> A group name is required to save. </Tip>
			{/if}
		</svelte:fragment>
	</Popup>

	{#if isNewGroup}
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
			<h3 class="h3">Group Editor</h3>
		</div>

		
		<div class="hidden md:flex space-x-2">
			<button class="button-neutral" on:click={drawerStore.close}>
				<Fa icon={faAngleDoubleRight} size="lg" />
				<span>Close</span>
			</button>

			<Popup autoPopupWidth={true}>
				<svelte:fragment slot="trigger">
					<button class="button-neutral" on:click={() => onUpdate()} disabled={!group.name}>
						<Fa icon={faFileCircleCheck} />
						Save
					</button>
				</svelte:fragment>
				<svelte:fragment slot="popup">
					{#if !group.name}
						<Tip customIcon={faWarning} tipTheme="tertiary"> A group name is required to save. </Tip>
					{/if}
				</svelte:fragment>
			</Popup>

			{#if isNewGroup}
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

			<TextInput label="Group" name="group" bind:value={group.name} />

		</div>

		<div class="space-y-2 ">
			<h4 class="h4">Members</h4>
			<JsonDropdownInput bind:dropdownList={membersMap} bind:data={selectedMembersMap} leadingIcon={faPerson}/>
		</div>

	</div>
</div>
