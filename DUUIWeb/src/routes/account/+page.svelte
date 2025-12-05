<script lang="ts">

	import { v4 as uuidv4 } from 'uuid'
	import { page } from '$app/stores'
	import { COLORS } from '$lib/config.js'
	import { errorToast, successToast } from '$lib/duui/utils/ui.js'
	import { groupStore, labelStore, userSession, registryStore } from '$lib/store.js'
	import Dropdown from '$lib/svelte/components/Input/Dropdown.svelte'
	import Secret from '$lib/svelte/components/Input/Secret.svelte'
	import Text from '$lib/svelte/components/Input/TextInput.svelte'
	import { showConfirmationModal } from '$lib/svelte/utils/modal.js'
	import { goto } from '$app/navigation'
	import {
		faAdd,
		faCheck,
		faExclamationCircle,
		faFileText,
		faGlobe,
		faInfoCircle,
		faLink, faPeopleGroup, faPerson, faRefresh,
		faTrash,
		faUser,
		faUserTie,
		faXmarkCircle
	} from '@fortawesome/free-solid-svg-icons'
	import {
		RadioGroup,
		RadioItem,
		clipboard,
		getModalStore,
		getToastStore,
		getDrawerStore
	} from '@skeletonlabs/skeleton'
	import { onMount } from 'svelte'
	import Fa from 'svelte-fa'
	import TextInput from "$lib/svelte/components/Input/TextInput.svelte";
	import { Accordion, AccordionItem } from '@skeletonlabs/skeleton';
	import FunctionalTable from '$lib/svelte/components/FunctionalTable.svelte'
	import Tip from "$lib/svelte/components/Tip.svelte";
	import _ from 'lodash';
	import { toTitleCase } from '$lib/duui/utils/text.js'
	import { IO, isStatelessProvider, type IOProvider } from '$lib/duui/io.js'
	import FunctionalTable2 from "$lib/svelte/components/FunctionalTable2.svelte";
	import Chip from "$lib/svelte/components/Chip.svelte";
	const { set, isEmpty } = _;

	const MAX_STORAGE_CONNECTIONS = 3

	const toastStore = getToastStore()
	const modalStore = getModalStore()
	const drawerStore = getDrawerStore()

	export let data
	let { user, dropbBoxURL, googleDriveURL, theme, users, labels, groups, settings, registries } = data

	const hasDropboxOAuthConfig =
		!!settings?.dbx_app_key && !!settings?.dbx_app_secret && !!settings?.dbx_redirect_url

	const hasGoogleOAuthConfig =
		!!settings?.google_client_id && !!settings?.google_client_secret && !!settings?.google_redirect_uri

	const themes = Object.keys(COLORS)

	if (user && $userSession) {
		$userSession.preferences = user.preferences
		$userSession.connections = user.connections
	}

	let email: string = $userSession?.email || ''
	let userPassword = ''
	let userPassword2 = ''
	let passwordError = false

	let minioConnections: {[name: string]:  MinioConnectionDetails} = $userSession?.connections.minio || {}
	let nextcloudConnections: {[name: string]:  NextcloudConnectionDetails} = $userSession?.connections.nextcloud || {}
	let googledriveConnections: {[name: string]:  OAuthConnectionDetails} = $userSession?.connections.google || {}
	let dropboxConnections: {[name: string]:  OAuthConnectionDetails} = $userSession?.connections.dropbox || {}

	let newNextcloud = {
		alias: "",
		uri: "",
		username: "",
		password: ""
	}

	let newDropboxConnection = {
		name: uuidv4(),
		alias: ""
	}

	let newGoogleConnection = {
		name: uuidv4(),
		alias: ""
	}

	let newMinioConnections = {
		alias: "",
		endpoint: "",
		access_key: "",
		secret_key: ""
	}



	$: isDropboxConnected = !$userSession ? false : Object.keys($userSession?.connections.dropbox).length > 0

	$: isMinioConnected = (name: string) =>  $userSession?.connections.minio[name].alias
			&& $userSession?.connections.minio[name].endpoint
			&& $userSession?.connections.minio[name].access_key
			&& $userSession?.connections.minio[name].secret_key

	$: isNextCloudConnected = (name: string) =>  $userSession?.connections.nextcloud[name].alias
			&& $userSession?.connections.nextcloud[name].uri
			&& $userSession?.connections.nextcloud[name].username
			&& $userSession?.connections.nextcloud[name].password

	$: isGoogleDriveConnected = !$userSession ? false : Object.keys($userSession?.connections.google).length > 0

	$: hasApiKey = !!$userSession?.connections.key

	$: {
		try {
			const body = document.body
			body.dataset.theme = 'theme-' + themes[theme]
		} catch (err) { /* empty */ }
	}

	let tab: number = +($page.url.searchParams.get('tab') || '0')

	onMount(() => {


		if ($userSession?.preferences.tutorial) {
			modalStore.trigger({
				type: 'component',
				component: 'helpModal'
			})
		}

		$labelStore = labels
		$groupStore = groups
		$registryStore = registries
		
		const newDropboxConnectionId = $page.url.searchParams.get("newDropboxConnectionId");
		if (newDropboxConnectionId && $userSession?.connections.dropbox[newDropboxConnectionId]) {
			updateUser({
				['connections.dropbox.' + $page.url.searchParams.get("newDropboxConnectionId") + '.alias']:
						localStorage.getItem("currentDropboxConnectionAlias")
			})
			$userSession.connections.dropbox[newDropboxConnectionId].alias =
					localStorage.getItem("currentDropboxConnectionAlias") || "";
			dropboxConnections = $userSession?.connections.dropbox

			removeQueryParam("newDropboxConnectionId")
			$page.url.searchParams.delete("newDropboxConnectionId")
			// window.location.href = $page.url.href
		}

		const newGoogleConnectionId = $page.url.searchParams.get("newGoogleConnectionId");
		if (newGoogleConnectionId && $userSession?.connections.google[newGoogleConnectionId]) {
			updateUser({
				['connections.google.' + $page.url.searchParams.get("newGoogleConnectionId") + '.alias']:
					localStorage.getItem("currentGoogleConnectionAlias")
			})

			$userSession.connections.google[newGoogleConnectionId].alias =
						localStorage.getItem("currentGoogleConnectionAlias") || ""

			googledriveConnections = $userSession?.connections.google

			removeQueryParam("newGoogleConnectionId")
			$page.url.searchParams.delete("newGoogleConnectionId")
			// window.location.href = $page.url.href
		}
	})

	const updateTheme = async (color: string) => {
		const response = await fetch(`/api/theme?color=${color}`, {
			method: 'PUT'
		})

		if (response.ok) {
			const result = await response.json()
			theme = +result.theme
		}
	}

	function checkAliasExists(connections: ServiceConnections, alias: string, name = ""): boolean {
		for (const [key, details] of Object.entries(connections)) {
			let isSameName = name === "" ? false :  name === key
			if (details.alias === alias && !isSameName) {
				toastStore.trigger(errorToast('This alias is already in use. Please choose a different one.'));
				return true;
			}
		}
		return false;
	}

	const insertConnection = async (provider: string, data: object, updateVar = () => {return;}) => {
		let req = { method: 'PUT', body: JSON.stringify(data) }

		const response = await fetch(`/api/users/connections?provider=${provider}`, req)

		if (response.ok) {
			updateVar()
			toastStore.trigger(successToast('Update successful'))
		} else {
			toastStore.trigger(errorToast('ERROR: \n' + JSON.stringify(await response.json())))
		}
		return response
	}

	const updateUser = async (data: object) => {
		let req = { method: 'PUT', body: JSON.stringify(data) }

		const response = await fetch('/api/users', req)

		if (response.ok) {
			toastStore.trigger(successToast('Update successful'))
		}
		return response
	}

	const updatePassword = async (pwd1: string | null | undefined =null, pwd2: string | null | undefined =null, oid: string | null | undefined =null) => {

		if (!oid) {
			oid = $userSession?.oid
		}

		if (!pwd1 || !pwd2) {
			pwd1 = userPassword
			pwd2 = userPassword2
		}

		const request = {
			method: 'POST',
			body: JSON.stringify({
				oid: oid,
				password1: pwd1,
				password2: pwd2
			}),
			headers: {
				'Content-Type': 'application/json'
			}
		}

		const response = await fetch('/auth/change', request)


		if (response.ok) {
			toastStore.trigger(successToast('Update successful'))
			passwordError = false
		} else {
			toastStore.trigger(errorToast(await response.json()))
			if (response.status === 422) passwordError = true
		}

	}

	const deleteAccount = async () => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Account',
				message:
					'Deleting your Account also deletes all pipelines and processes ever created. Are you sure?',
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return

		await fetch('/api/users', {
			method: 'DELETE'
		})
	}

	const generateApiKey = async () => {
		if (user.connections.key) {
			const confirm = await showConfirmationModal(
				{
					title: 'Regenerate API Key',
					message:
						'If you regenarate your API key, the current one will not work anymore. Make sure to update your API key in all applications its used in.',
					textYes: 'Regenerate'
				},
				modalStore
			)

			if (!confirm) return
		}

		const response = await fetch('/api/users/auth/key', { method: 'PUT' })

		if (response.ok && $userSession) {
			const item = await response.json()
			$userSession.connections.key = item.user.connections.key
		}
	}

	const deleteApiKey = async () => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete API Key',
				message:
					'Deleting your API Key remove the ability to make requests with it. You can always generate a new one here.',
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return

		const response = await fetch('/api/users/auth/key', { method: 'DELETE' })

		if (response.ok && $userSession) {
			$userSession.connections.key = null
		}
	}

	const startDropboxOauth = (alias: string) => {
		localStorage.setItem("currentDropboxConnectionAlias", alias)
		window.location.href = dropbBoxURL.toString()
	}

	const deleteDropboxAccess = async (name: string) => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Access for Dropbox',
				message: `Are you sure you want to revoke access?
					 You will have to go through the OAuth process again to reconnect.`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return

		delete dropboxConnections[name]
		const response = await updateUser({
			['connections.dropbox']: dropboxConnections
		})

		if (response.ok && $userSession?.connections.dropbox) {
			dropboxConnections = $userSession?.connections.dropbox
		}
	}

	const startGoogleDriveAccess = async (alias: string) => {
		localStorage.setItem("currentGoogleConnectionAlias", alias)
		window.location.href = googleDriveURL
	}

	const deleteGoogleDriveAccess = async (name: string) => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Access for Google Drive',
				message: `Are you sure you want to revoke access?
					 You will have to go through the OAuth process again to reconnect.`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return

		delete googledriveConnections[name]
		const response = await updateUser({
			'connections.google': googledriveConnections
		})

		if (response.ok && $userSession && $userSession.connections.google) {
			googledriveConnections = $userSession?.connections.google
		}
	}

	function removeQueryParam(param) {
		// Get the current URL
		const url = new URL(window.location.href);

		// Get the query parameters
		const searchParams = new URLSearchParams(url.search);

		// Remove the specified parameter
		searchParams.delete(param);

		// Update the URL without reloading the page
		const newUrl = `${url.origin}${url.pathname}?${searchParams.toString()}`;
		window.history.replaceState({}, '', newUrl);
	}

	const revokeMinioAccess = async (name: string) => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Access for Min.io',
				message: `Are you sure you want to delete access?`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return

		delete minioConnections[name]
		const response = await updateUser({
			'connections.minio': minioConnections
		})

		if (response.ok) {
			if ($userSession) {
				minioConnections = $userSession?.connections.minio
			}
		}
	}

	const revokeNextcloudAccess = async (name: string) => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Access for NextCloud',
				message: `Are you sure you want to delete access?`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return

		delete nextcloudConnections[name]
		const response = await updateUser({
			'connections.nextcloud': nextcloudConnections
		})

		if (response.ok) {
			nextcloudConnections = $userSession?.connections.nextcloud || {}
		}
	}

	const deleteUser = async (user: { oid: string; email: string; role: string }) => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Account',
				message:
					'Deleting the Account also deletes all pipelines and processes ever created. Are you sure?',
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return

		const response = await fetch('/api/users/admin', {
			method: 'DELETE',
			body: JSON.stringify(user)
		})

		if (response.ok) {
			users = users.filter((item: { oid: string }) => item.oid !== user.oid)
		}
	}

	const updateRole = async (user: { oid: string; email: string; role: string }) => {
		const response = await fetch('/api/users/admin', { method: 'PUT', body: JSON.stringify(user) })
		if (response.ok) {
			toastStore.trigger(successToast('Update successful'))
		}
	}

	const setTab = (newTab: number) => {
		tab = newTab
		goto('account?tab=' + tab)
	}
</script>

<svelte:head>
	<title>Account</title>
</svelte:head>

<div class="menu-mobile-lg !w-screen">
	<button
		on:click={() => {
			setTab(0)
		}}
		class="button-mobile {tab === 0 ? '!variant-filled-primary' : ''}"
	>
		<Fa icon={faUser} />
		<span>Profile</span>
	</button>

	<button
		on:click={() => {
			setTab(1)
		}}
		class="button-mobile {tab === 1 ? '!variant-filled-primary' : ''}"
	>
		<Fa icon={faLink} />
		<span>Connections</span>
	</button>

	{#if $userSession?.role === 'Admin'}
		<button
			on:click={() => {
				setTab(2)
			}}
			class="button-mobile {tab === 2 ? '!variant-filled-primary' : ''}"
		>
			<Fa icon={faUserTie} />
			<span>Admin</span>
		</button>
		<button
				on:click={() => {
				setTab(3)
			}}
				class="button-mobile {tab === 3 ? '!variant-filled-primary' : ''}"
		>
			<Fa icon={faGlobe} />
			<span>Globals</span>
		</button>
	{/if}
</div>

<div class="flex lg:flex-row flex-col h-full pb-16 lg:p-16 gap-4">
	<div class="sticky top-4 lg:top-20 self-start section-wrapper p-8 hidden min-w-[20%] lg:block">
		<div class="grid gap-4">
			<button
				on:click={() => {
					setTab(0)
				}}
				class="button-neutral border-none {tab === 0 ? '!variant-filled-primary' : ''}"
			>
				<Fa icon={faUser} />
				<span>Profile</span>
			</button>

			<button
				on:click={() => {
					setTab(1)
				}}
				class="button-neutral border-none {tab === 1 ? '!variant-filled-primary' : ''}"
			>
				<Fa icon={faLink} />
				<span>Connections</span>
			</button>

			{#if $userSession?.role === 'Admin'}
				<button
					on:click={() => {
						setTab(2)
					}}
					class="button-neutral border-none {tab === 2 ? '!variant-filled-primary' : ''}"
				>
					<Fa icon={faUserTie} />
					<span>Admin</span>
				</button>

				<button
						on:click={() => {
						setTab(3)
					}}
						class="button-neutral border-none {tab === 3 ? '!variant-filled-primary' : ''}"
				>
					<Fa icon={faGlobe} />
					<span>Globals</span>
				</button>
			{/if}
		</div>
	</div>
	<div class="md:min-w-[800px] space-y-4">
		{#if tab === 0}
			<div class="space-y-4">
				<div class="section-wrapper p-8 space-y-8">
					<h2 class="h3">Profile</h2>
					<Text label="E-Mail" name="email" readonly={true} bind:value={email} />
					<Secret label="Password" name="password" disabled={false} bind:value={userPassword} />
					<Secret label="Repeat Password" name="password2" disabled={false} style={passwordError ? "text-red-500 border-4 border-red-400 rounded-lg px-2 py-1" : ""} bind:value={userPassword2} />

					<div class="label">
						<p class="form-label">Theme</p>
						<RadioGroup
							class="grid grid-cols-2 gap-2 p-2 section-wrapper w-full"
							active="variant-filled-primary"
							padding="p-4"
						>
							<RadioItem
								bind:group={theme}
								name="blue"
								value={0}
								on:click={() => updateTheme('blue')}>Blue</RadioItem
							>
							<RadioItem bind:group={theme} name="red" value={1} on:click={() => updateTheme('red')}
								>Red</RadioItem
							>
							<RadioItem
								bind:group={theme}
								name="purple"
								value={2}
								on:click={() => updateTheme('purple')}>Purple</RadioItem
							>
							<RadioItem
								bind:group={theme}
								name="green"
								value={3}
								on:click={() => updateTheme('green')}>Green</RadioItem
							>
						</RadioGroup>
					</div>
					<hr class="hr !w-full" />
					<div class="grid md:flex justify-between gap-4">
						<button class="button-primary" on:click={() => updatePassword(userPassword, userPassword2)}>
							<Fa icon={faRefresh} />
							<span>Update Password</span>
						</button>
						<button class="button-error" on:click={deleteAccount}>
							<Fa icon={faTrash} />
							<span>Delete Account</span>
						</button>
					</div>
				</div>
			</div>

		{:else if tab === 1}
			<div class="space-y-4">
				<div class="section-wrapper p-8 space-y-8 scroll-mt-16" id="authorization">
					<h2 class="h3">API Key</h2>
					<div class="space-y-8">
						{#if hasApiKey}
							<div class="space-y-2">
								<Secret value={$userSession?.connections.key} style={"pt-2"} />
								<div
									class="grid grid-cols-3 items-center justify-center section-wrapper divide-x divider text-sm"
								>
									<button
										class="button-neutral !justify-center !border-none !rounded-none"
										use:clipboard={$userSession?.connections.key || ''}
										on:click={() => {
											toastStore.trigger(successToast('Copied!'))
										}}
									>
										Copy
									</button>
									<button
										class="button-neutral !justify-center !border-y-0 !rounded-none !border-x border-color"
										on:click={generateApiKey}
									>
										Regenerate
									</button>
									<button
										class="button-neutral !justify-center !border-none !rounded-none"
										on:click={deleteApiKey}
									>
										Delete
									</button>
								</div>
							</div>

							<p class="text-surface-500 dark:text-surface-200">
								Don't share this key! Anyone with this key can make api calls in your name.
							</p>
						{:else}
							<p class="text-surface-500 dark:text-surface-200">Generate a key to use the Api.</p>
							<button class="button-primary" on:click={generateApiKey}>
								<Fa icon={faAdd} />
								<span>Generate</span>
							</button>
						{/if}
					</div>
					<p class="text-surface-500 dark:text-surface-200">
						Check the
						<a href="/documentation/api" target="_blank" class="anchor">API Reference</a>
						for examples and use cases.
					</p>
				</div>
				<Accordion class="space-y-4" id="connections">
					<div class="section-wrapper rounded-md" id="dropbox">
						<AccordionItem open disabled={!hasDropboxOAuthConfig}>
							<svelte:fragment slot="summary">
								<div class="flex items-center gap-2">
									<b class="h2 scroll-mt-16 text-primary-500">
										Dropbox
									</b>
									{#if !hasDropboxOAuthConfig}
										<span class="badge variant-soft-error font-bold">
											NOT AVAILABLE
										</span>
									{/if}
								</div>
							</svelte:fragment>
							<svelte:fragment slot="content">
								{#if !hasDropboxOAuthConfig}
									<div class="p-8">
										<Tip tipTheme="error" customIcon={faInfoCircle}>
											This storage connection is currently unavailable.
										</Tip>
									</div>
								{:else}
									<div class=" p-8 grid grid-rows-[auto_1fr_auto] gap-8">
										<div class="space-y-8">
											<div>
												{#if isDropboxConnected}
													<p>Your Dropbox account has been connected successfully.</p>
													<p>
														The folder <span class="badge px-2 mx-2 variant-soft-primary"
															>Apps/Docker Unified UIMA Interface</span
														> has been created.
													</p>
												{:else}
													<p class="mb-8">
														By connecting Dropbox and DUUI you can directly read and write data from and to your
														Dropbox storage. After a succesfull OAuth2 authorization at <span class="font-bold"
													>Dropbox</span
													> an app folder called DUUI is created in your storage that is used as the root folder
														for read and write operations.
													</p>
												{/if}
											</div>
											<div>
												<p class="flex-center-4">
													<Fa icon={isDropboxConnected ?  faCheck : faFileText} size="lg" class="text-primary-500" />
													<span>
														Read files and folders contained in your <strong>Dropbox Account</strong>
													</span>
												</p>
												<p class="flex-center-4 mb-4">
													<Fa icon={isDropboxConnected ?  faCheck : faFileText} size="lg" class="text-primary-500" />
													<span>Create files and folders in your <strong>Dropbox Account</strong> </span>
												</p>
											</div>
											{#each Object.entries(dropboxConnections) as [name, _]}
												<div class="bordered-soft rounded-md overflow-hidden">
													<div class="space-y-4 p-4">
														<TextInput
															label="Alias"
															style={"grow"}
															name={name}
															bind:value={dropboxConnections[name].alias}
														/>
														<div class="grid md:flex justify-between gap-4">
															<button class="button-neutral" disabled={!dropboxConnections[name].alias}
																	on:click={() => {
																		if (checkAliasExists(dropboxConnections, dropboxConnections[name].alias, name))
																			return;

																		updateUser({
																			['connections.dropbox.' + name]: {
																				alias: dropboxConnections[name].alias,
																			}
																		})
															}}>
																<Fa icon={faLink} />
																<span>Save</span>
															</button>
															<button class="button-error" on:click={() => deleteDropboxAccess(name)}>
																<Fa icon={faXmarkCircle} />
																<span>Delete</span>
															</button>
														</div>
													</div>
												</div>
											{/each}
											<div class="space-y-2">
												<h3>Add New Connection</h3>
												<div class="grid md:flex justify-between gap-4 mb-4">
													<TextInput
														label="Alias"
														style={"grow"}
														name="newDropboxAlias"
														bind:value={newDropboxConnection.alias}
													/>
												</div>
												<button class="button-neutral" disabled={!newDropboxConnection.alias || Object.keys(dropboxConnections || {}).length >= MAX_STORAGE_CONNECTIONS} on:click={() => {
														if (checkAliasExists(dropboxConnections, newDropboxConnection.alias))
															return;
														startDropboxOauth(newDropboxConnection.alias)
													}
													}>
													<Fa icon={faLink} />
													<span>Connect</span>
												</button>
											</div>
											{#if Object.keys(dropboxConnections || {}).length >= MAX_STORAGE_CONNECTIONS}
												<Tip customIcon={faExclamationCircle} tipTheme="warning">
													Maximum number of storage connections reached.
												</Tip>
											{/if}
											<p class="text-surface-500 dark:text-surface-200">
												Visit
												<a
													href="https://help.dropbox.com/de-de/integrations/third-party-apps"
													target="_blank"
													class="anchor">Dropbox Apps</a
												>
												for further reading.
											</p>
										</div>
									</div>
								{/if}
							</svelte:fragment>
						</AccordionItem>
					</div>

					<div class="section-wrapper rounded-md" id="minio">
						<AccordionItem open>
							<svelte:fragment slot="summary">
								<b class="h2 scroll-mt-16 text-primary-500">
									Minio / AWS
								</b>	
							</svelte:fragment>
							<svelte:fragment slot="content">
	
								<div class="p-8 grid grid-rows-[auto_1fr_auto] gap-8">
	
									{#if Object.keys($userSession?.connections.minio || {}).length > 1}
										<p>Your account has been connected to Minio / AWS successfully.</p>
									{:else}
										<p>Enter your AWS credentials below to establish a connection.</p>
									{/if}
									{#each Object.entries(minioConnections) as [name, _]}
										<div class="bordered-soft rounded-md overflow-hidden">
											<div class="space-y-4 p-4">
												<Text
														label="Alias"
														style={"grow"}
														name={name}
														bind:value={minioConnections[name].alias}
												/>
												<Text
													help="The correct endpoint is the s3 API endpoint. Do not use the Minio Console endpoint!"
													label="Endpoint"
													style={"grow"}
													name="endpoint"
													bind:value={minioConnections[name].endpoint}
												/>
												<Secret label="Username (Access Key)" name="accessKey" bind:value={minioConnections[name].access_key} />
												<Secret label="Password (Secret Key)" name="secretKey" bind:value={minioConnections[name].secret_key} />
											</div>
	
	
											<div class="grid md:flex justify-between gap-4">
												<button
													class="button-neutral"
													disabled={!minioConnections[name].endpoint || !minioConnections[name].access_key || !minioConnections[name].secret_key}
													on:click={() => {
														if (checkAliasExists(minioConnections, minioConnections[name].alias, name))
															return;
														insertConnection("minio", {
															['connections.minio.' + name]: {
																endpoint: minioConnections[name].endpoint,
																access_key: minioConnections[name].access_key,
																secret_key: minioConnections[name].secret_key
															}
														})
	
													}}
												>
													<Fa icon={isMinioConnected(name) ? faRefresh : faLink} />
													<span>{'Update'}</span>
												</button>
												{#if isMinioConnected(name)}
													<button class="button-error" on:click={() => revokeMinioAccess(name)}>
														<Fa icon={faXmarkCircle} />
														<span>Delete</span>
													</button>
												{/if}
											</div>
										</div>
									{/each}
									<div class="bordered-soft rounded-md overflow-hidden p-4">
										<div class="space-y-4 mb-4">
											<Text
													label="Alias"
													style={"grow"}
													name="newAlias"
													bind:value={newMinioConnections.alias}
											/>
											<Text
													label="Endpoint"
													style={"grow"}
													name="endpoint"
													bind:value={newMinioConnections.endpoint}
											/>
											<Secret label="Username (Access Key)" name="accessKey" bind:value={newMinioConnections.access_key} />
											<Secret label="Password (Secret Key)" name="secretKey" bind:value={newMinioConnections.secret_key} />
										</div>
										<div class="grid md:flex justify-between gap-4">
											<button
													class="button-neutral"
													disabled={!newMinioConnections.endpoint || !newMinioConnections.access_key || !newMinioConnections.secret_key || !newMinioConnections.alias || Object.keys(minioConnections || {}).length >= MAX_STORAGE_CONNECTIONS}
													on:click={() => {
														if (checkAliasExists(minioConnections, newMinioConnections.alias))
															return;
	
														let newMinioId  = uuidv4()
														const updateVar = () => {
															minioConnections[newMinioId] = newMinioConnections
															newMinioConnections = { alias: "", endpoint: "", secret_key: "", access_key: "" }
														}
														insertConnection("minio", {
															['connections.minio.' + newMinioId]: {
																alias: newMinioConnections.alias,
																endpoint: newMinioConnections.endpoint,
																access_key: newMinioConnections.access_key,
																secret_key: newMinioConnections.secret_key
															}
														}, updateVar)
	
													}}
											>
												<Fa icon={faLink} />
												<span>Connect</span>
											</button>
										</div>
										{#if Object.keys(minioConnections || {}).length >= MAX_STORAGE_CONNECTIONS}
											<Tip customIcon={faExclamationCircle} tipTheme="warning">
												Maximum number of storage connections reached.
											</Tip>
										{/if}
									</div>
	
									<p class="text-surface-500 dark:text-surface-200">
										Visit
										<a href="https://min.io/" target="_blank" class="anchor">Minio</a>
										for further reading.
									</p>
								</div>
							</svelte:fragment>
						</AccordionItem>
					</div>

					<div class="section-wrapper rounded-md" id="nextcloud">
						<AccordionItem open>
							<svelte:fragment slot="summary">
								<b class="h2 scroll-mt-16 text-primary-500">
									NextCloud
								</b>	
							</svelte:fragment>
							<svelte:fragment slot="content">
								<div class="p-8 grid grid-rows-[auto_1fr_auto] gap-8">
									{#if Object.keys($userSession?.connections.nextcloud || {}).length > 1}
										<p>Your accounts has been connected to NextCloud successfully.</p>
									{:else}
										<p>Enter your NextCloud credentials below to establish a connection.</p>
									{/if}
	
								{#each Object.entries($userSession?.connections.nextcloud || {}) as [name, _]}
									<div class="bordered-soft rounded-md overflow-hidden p-4">
										<div class="space-y-4 mb-4">
											<Text
												label="Alias"
												style={"grow"}
												name={name}
												bind:value={nextcloudConnections[name].alias}
											/>
											<Text
												label="Endpoint"
												style={"grow"}
												name="endpoint"
												bind:value={nextcloudConnections[name].uri}
											/>
											<Secret label="Username (Access Key)" name="accessKey" bind:value={nextcloudConnections[name].username} />
											<Secret label="Password (Secret Key)" name="secretKey" bind:value={nextcloudConnections[name].password} />
										</div>
										<div class="grid md:flex justify-between gap-4">
											<button
												class="button-neutral"
												disabled={!isNextCloudConnected(name)}
												on:click={() => {
													if (checkAliasExists(nextcloudConnections, nextcloudConnections[name].alias, name))
														return;
													insertConnection("nextcloud", {
														['connections.nextcloud.' + name]: {
															alias: nextcloudConnections[name].alias,
															uri: nextcloudConnections[name].uri,
															username: nextcloudConnections[name].username,
															password: nextcloudConnections[name].password
														}
													})
												}}
											>
												<Fa icon={isNextCloudConnected(name) ? faRefresh : faLink} />
												<span>{isNextCloudConnected(name) ? 'Update' : 'Connect'}</span>
											</button>
											{#if isNextCloudConnected(name)}
												<button class="button-error" on:click={() => revokeNextcloudAccess(name)}>
													<Fa icon={faXmarkCircle} />
													<span>Delete</span>
												</button>
											{/if}
										</div>
									</div>
								{/each}
								<div class="bordered-soft rounded-md overflow-hidden p-4">
									<div class="space-y-4 mb-4">
										<Text
												label="Alias"
												style={"grow"}
												name="newAlias"
												bind:value={newNextcloud.alias}
										/>
										<Text
												label="Endpoint"
												style={"grow"}
												name="endpoint"
												bind:value={newNextcloud.uri}
										/>
										<Secret label="Username (Access Key)" name="accessKey" bind:value={newNextcloud.username} />
										<Secret label="Password (Secret Key)" name="secretKey" bind:value={newNextcloud.password} />
									</div>
									<div class="grid md:flex justify-between gap-4">
										<button
												class="button-neutral"
												disabled={!newNextcloud.uri || !newNextcloud.username || !newNextcloud.password || !newNextcloud.alias || Object.keys(nextcloudConnections || {}).length >= MAX_STORAGE_CONNECTIONS}
												on:click={() => {
													if (checkAliasExists(nextcloudConnections, newNextcloud.alias))
														return;
	
													let newId  = uuidv4()
													const updateVar = () => {
														nextcloudConnections[newId] = newNextcloud
														newNextcloud = { alias: "", uri: "", password: "", username: "" }
													}
													insertConnection("nextcloud", {
														['connections.nextcloud.' + newId]: {
															alias: newNextcloud.alias,
															uri: newNextcloud.uri,
															username: newNextcloud.username,
															password: newNextcloud.password
														}
													}, updateVar)
	
												}}
										>
											<Fa icon={faLink} />
											<span>Connect</span>
										</button>
									</div>
									{#if Object.keys(nextcloudConnections || {}).length >= MAX_STORAGE_CONNECTIONS}
										<Tip customIcon={faExclamationCircle} tipTheme="warning">
											Maximum number of storage connections reached.
										</Tip>
									{/if}
								</div>
	
	
			<!--					<p class="text-surface-500 dark:text-surface-200">-->
			<!--						Visit-->
			<!--						<a href="https://min.io/" target="_blank" class="anchor">NextCloud</a>-->
			<!--						for further reading.-->
			<!--					</p>-->
							</div>
							</svelte:fragment>
						</AccordionItem>
					</div>

					<div class="section-wrapper rounded-md" id="google">
						<AccordionItem open disabled={!hasGoogleOAuthConfig}>
							<svelte:fragment slot="summary">
								<div class="flex items-center gap-2">
									<b class="h2 scroll-mt-16 text-primary-500">
										Google Drive
									</b>
									{#if !hasGoogleOAuthConfig}
										<span class="badge variant-soft-error font-bold">
											NOT AVAILABLE
										</span>
									{/if}
								</div>
							</svelte:fragment>
							<svelte:fragment slot="content">
								{#if !hasGoogleOAuthConfig}
									<div class="p-8">
										<Tip tipTheme="error" customIcon={faInfoCircle}>
											This storage connection is currently unavailable.
										</Tip>
									</div>
								{:else}
									<div class="p-8 grid grid-rows-[auto_1fr_auto] gap-8">
										<div class="space-y-8">
											{#if isGoogleDriveConnected}
												<div>
													<p>Your Google Drive account has been connected successfully.</p>
													<p>
														The folder <span class="badge px-2 mx-2 variant-soft-primary"
													>Apps/Docker Unified UIMA Interface</span
													> has been created.
													</p>
												</div>
											{:else}
												<div>
													<p class="mb-8">
														By connecting GoogleDrive and DUUI you can directly read and write data from and to your
														GoogleDrive storage. After a succesfull OAuth2 authorization at <span class="font-bold"
													>GoogleDrive</span
													> an app folder called DUUI is created in your storage that is used as the root folder
														for read and write operations.
													</p>
												</div>
											{/if}
											<div>
												<p class="flex-center-4">
													<Fa icon={isGoogleDriveConnected ? faCheck: faFileText} size="lg" class="text-primary-500" />
													<span
													>Read files and folders contained in your <strong>Google Drive Account</strong>
													</span>
												</p>
												<p class="flex-center-4 mb-4">
													<Fa icon={isGoogleDriveConnected ? faCheck: faFileText} size="lg" class="text-primary-500" />
													<span>Create files and folders in your <strong>Google Drive Account</strong> </span>
												</p>
											</div>
											{#each Object.entries(googledriveConnections) as [name, _]}
												<div class="bordered-soft rounded-md overflow-hidden">
													<div class="space-y-4 p-4">
														<TextInput
																label="Alias"
																style={"grow"}
																name={name}
																placeholder="Input alias "
																bind:value={googledriveConnections[name].alias}
														/>
														<div class="grid md:flex justify-between gap-4">
															<button class="button-neutral" disabled={!googledriveConnections[name].alias}
																	on:click={() => {
																if (checkAliasExists(googledriveConnections, googledriveConnections[name].alias, name))
																	return;

																updateUser({
																	['connections.google.' + name]: {
																		alias: googledriveConnections[name].alias,
																	}
																})
														}}>
																<Fa icon={faLink} />
																<span>Save</span>
															</button>
															<button class="button-error" on:click={() => deleteGoogleDriveAccess(name)}>
																<Fa icon={faXmarkCircle} />
																<span>Delete</span>
															</button>
														</div>
													</div>
												</div>
											{/each}

											<div class="space-y-2">
												<h3>Add New Connection</h3>
												<div class="grid md:flex justify-between gap-4 mb-4">
													<TextInput
															label="Alias"
															style={"grow"}
															name="newGoogleAlias"
															placeholder="Input alias "
															bind:value={newGoogleConnection.alias}
													/>
												</div>
												<button class="button-neutral" disabled={!newGoogleConnection.alias || Object.keys(googledriveConnections || {}).length >= MAX_STORAGE_CONNECTIONS} on:click={() => {
													if (checkAliasExists(googledriveConnections, newGoogleConnection.alias))
														return;
													startGoogleDriveAccess(newGoogleConnection.alias)
												}}>
													<Fa icon={faLink} />
													<span>Connect</span>
												</button>
											</div>
											{#if Object.keys(googledriveConnections || {}).length >= MAX_STORAGE_CONNECTIONS}
												<Tip customIcon={faExclamationCircle} tipTheme="warning">
													Maximum number of storage connections reached.
												</Tip>
											{/if}
										</div>
									</div>
								{/if}
							</svelte:fragment>
						</AccordionItem>
					</div>
				</Accordion>
			</div>
		{:else if tab === 2}
			{#if $userSession?.role === 'Admin'}
				<div class="section-wrapper p-8 space-y-4 w-full">
					<h3 class="h3">Users</h3>

					{#if users}
						<div class="bordered-soft rounded-md overflow-hidden max-w-full">
							<div class="overflow-x-auto">
								<div
										class="grid w-full grid-cols-[1fr_1fr_1fr_1fr_1fr_1fr] lg:grid-cols-[1fr_1fr_1fr_1fr_1fr_1fr] bg-surface-100-800-token gap-0.5 p-2"
								>
									<p class="p-4 break-words max-w-[10ch] md:max-w-none">Email</p>
									<p class="p-4 hidden lg:block text-center">Role</p>
									<p class="p-4 hidden lg:block text-center">Workers</p>
									<p class="p-4 hidden lg:block text-center">Password</p>
								</div>

								{#each users as _user}
									<div
											class="grid w-full grid-cols-[1fr_1fr_1fr_auto_auto_auto] lg:grid-cols-[1fr_1fr_1fr_auto_auto_auto]  border-t border-color gap-0.5 p-1"
									>
										<p class="px-4 py-2 self-center">{_user.email}</p>
										<Dropdown
												name={_user.email}
												offset={0}
												on:change={() => updateRole(_user)}
												bind:value={_user.role}
												style={"button-menu px-4 py-2 !self-stretch h-full"}
												border="border-y-0 !border-x border-color"
												options={['User', 'Admin', 'Trial']}
										/>
										<p class="hidden lg:block px-4 py-2 self-center">{_user.worker_count}</p>
										<Secret
												name="password"
												style={"button-menu px-4 py-2 !self-stretch h-full border-y-0 !border-x border-color"}
												disabled={false}
												bind:value={_user.password}
										/>
										<button
												class="h-10 px-4 py-2 button-menu self-center hover:!variant-soft-primary transition-300 !border-r-0 !border-l-0 !border-x border-color"
												on:click={() => updatePassword(_user.password, _user.password, _user.oid)}
										>
	<!--											on:click={() => alert(JSON.stringify(_user))}-->

											<Fa icon={faRefresh} />
											<span>Update Password</span>
										</button>
										<button
												class="h-10 px-4 py-2 button-menu self-center hover:!variant-soft-error transition-300 border-l !border-r-0 !border-x border-color"
												on:click={() => deleteUser(_user)}
										>
											<Fa icon={faTrash} />
											<span class="hidden md:inline">Delete</span>
										</button>
									</div>
								{/each}
							</div>
						</div>
					{:else}
						<Tip tipTheme={ "tertiary" } >
							There are currently no additional users to manage.
						</Tip>
					{/if}
				</div>

			{/if}

		{:else if tab === 3}
			{#if $userSession?.role === 'Admin'}
				<div class="section-wrapper p-8 space-y-4 w-full">
					{#if users && users.length > 0 && $groupStore !== undefined}
						<FunctionalTable2
							title="Groups"
							columns={{ name: "Name", members: "Members" }}
							data={$groupStore}
							on:edit={(e) => {
								const id = e.detail.id;
								if (!$groupStore?.[id]) return;
								drawerStore.open({
									id: 'group',
									width: 'w-full lg:w-[60%] h-full',
									position: 'right',
									rounded: 'rounded-none',
									border: 'border-l border-color',
									meta: {
										group: $groupStore[id],
										groupId: id,
										creating: false,
										users
									}
								});
							}}
							on:add={() => {
								if (!$groupStore) return;

								const newId = uuidv4();
								$groupStore[newId] = { name: "", members: [], whitelist: [] };

								drawerStore.open({
									id: 'group',
									width: 'w-full lg:w-[60%] h-full',
									position: 'right',
									rounded: 'rounded-none',
									border: 'border-l border-color',
									meta: {
										group: $groupStore[newId],
										groupId: newId,
										creating: true,
										users
									}
								});
							}}
						>
							<svelte:fragment slot="cell" let:row let:colKey let:value>
								{#if colKey === 'members'}
									<div class="flex flex-wrap gap-1 p-1">
										{#each /** cast to array **/ value as memberId}
											{@const user = users.find(u => u.oid === memberId)}
											<Chip
													text={user ? user.email : memberId}
													leftIcon={faPerson}
											/>
										{/each}
									</div>
								{:else}
									<p class="p-4 break-words text-center">{value ?? ''}</p>
								{/if}
							</svelte:fragment>
						</FunctionalTable2>
					{:else}
						<Tip>Please register users to manage Groups.</Tip>
					{/if}
					<FunctionalTable2
							title="Labels"
							columns={{ label: "Name", driver: "Driver", groups: "Groups" }}
							data={$labelStore}
							on:edit={(e) => {
							drawerStore.open({
							  id: 'label',
							  width: 'w-full lg:w-[60%] h-full',
							  position: 'right',
							  rounded: 'rounded-none',
							  border: 'border-l border-color',
							  meta: {
								creating: false,
								labelId: e.detail.id,
								label: $labelStore?.[e.detail.id] ?? {}
							  }
							});
						  }}
													on:add={() => {
							drawerStore.open({
							  id: 'label',
							  width: 'w-full lg:w-[60%] h-full',
							  position: 'right',
							  rounded: 'rounded-none',
							  border: 'border-l border-color',
							  meta: {
								creating: true,
								labelId: uuidv4(),
								label: { label: "", driver: "Any" }
							  }
							});
						  }}
					>
						<!-- customize only the “groups” list column -->
						<svelte:fragment slot="cell" let:row let:colKey let:value>
							{#if colKey === 'groups'}
								<div class="flex flex-wrap gap-1 p-1">
									{#each /** cast to array **/ value as groupId}
										<Chip
												text={groups[groupId]?.name ?? groupId}
												leftIcon={faPeopleGroup}
										/>
									{/each}
								</div>
							{:else}
								<p class="p-4 break-words text-center">{value ?? ''}</p>

							{/if}
						</svelte:fragment>
					</FunctionalTable2>


					<FunctionalTable2
							title="Registries"
							columns={{ name: "Name", endpoint: "Endpoint", scope: "Scope" }}
							data={$registryStore}
							on:edit={(e) => {
							drawerStore.open({
							  id: 'registry',
							  width: 'w-full lg:w-[60%] h-full',
							  position: 'right',
							  rounded: 'rounded-none',
							  border: 'border-l border-color',
							  meta: {
								creating: false,
								registryId: e.detail.id,
								registry: $registryStore?.[e.detail.id] ?? {}
							  }
							});
						  }}
													on:add={() => {
							drawerStore.open({
							  id: 'registry',
							  width: 'w-full lg:w-[60%] h-full',
							  position: 'right',
							  rounded: 'rounded-none',
							  border: 'border-l border-color',
							  meta: {
								creating: true,
								registryId: uuidv4(),
								registry: {
								  name: "",
								  endpoint: "",
								  scope: "USER",
								  groups: []
								}
							  }
							});
						  }}
					>
					</FunctionalTable2>

					<div class="section-wrapper p-4 space-y-4">
						<h3 class="h3">Global Setting</h3>
						<p class="text-surface-500 dark:text-surface-200">
							Set global configuration settings for this DUUI-Gateway instance. 
							These settings will be saved to the database.
						</p>
						
						<div class="ml-2">
							<!-- Add Button -->
							<button class="btn variant-filled-primary" on:click={() => 
								drawerStore.open({
									id: 'settings',
									width: 'w-full lg:w-[60%] h-full',
									position: 'right',
									rounded: 'rounded-none',
									border: 'border-l border-color',
									meta: { 
										settings: settings
									}
								})}>
								<Fa icon="plus" class="mr-2"/>
								Edit
							</button>
						</div>
					</div>
				</div>
			{/if}
		{/if}
	</div>
</div>

<style>
	p {
		max-width: 65ch;
	}
</style>
