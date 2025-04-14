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
	import { get } from 'svelte/store'
	import JsonDropdownInput from '../Input/JsonDropdownInput.svelte'
	import Chips from '../Input/Chips.svelte'

	const drawerStore = getDrawerStore()
	
	
	let settings: DUUISettings = $drawerStore.meta.settings

	const toastStore = getToastStore()


	const onUpdate = async () => {

		const response = await fetch('/api/settings/', {
			method: 'PUT',
			body: JSON.stringify({...settings }),
		})

		if (response.ok) {
			toastStore.trigger(successToast('Settings updated successfully'))
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

	<button class="button-mobile" on:click={() => onUpdate()}>
		<Fa icon={faFileCircleCheck} />
		Save
	</button>
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

			<button class="button-neutral" on:click={() => onUpdate()}>
				<Fa icon={faFileCircleCheck} />
				Save
			</button>
		</div>
	</div>

	<div class="space-y-8 p-4 bg-surface-50-900-token pb-16 md:pb-4">
		<div class="space-y-4">
			<h4 class="h4">Dropbox Settings</h4>

			<TextInput label="Redirect URL" name="dbx_redirect_url" bind:value={settings.dbx_redirect_url} />
			<TextInput label="App Key" name="dbx_app_key" bind:value={settings.dbx_app_key} />
			<TextInput label="App Secret" name="dbx_app_secret" bind:value={settings.dbx_app_secret} />

		</div>
		
		<div class="space-y-4">
			<h4 class="h4">Google Settings</h4>

			<TextInput label="Redirect URI" name="google_redirect_uri" bind:value={settings.google_redirect_uri} />
			<TextInput label="Client ID" name="google_client_id" bind:value={settings.google_client_id} />
			<TextInput label="Client Secret" name="google_client_secret" bind:value={settings.google_client_secret} />
			
		</div>
		
		<div class="space-y-2">
			<TextInput label="File Upload Directory" name="file_upload_directory" bind:value={settings.file_upload_directory} />
		</div>
		
		<div class="space-y-2">
			<Chips style="md:col-span-2" label="Allowed Origins" bind:values={settings.allowed_origins} />
		</div>
	</div>
</div>