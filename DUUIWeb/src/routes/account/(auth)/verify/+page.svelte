<script lang="ts">
	import {
		faArrowRightFromBracket,
		faEnvelope,
	} from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'
	import { getModalStore, getToastStore } from '@skeletonlabs/skeleton'
	import { showCodeModal } from '$lib/svelte/utils/modal'
	import { errorToast, infoToast, successToast } from '$lib/duui/utils/ui'
	import { userSession } from '$lib/store'
	import { goto } from '$app/navigation'

	let user = $userSession

	const toastStore = getToastStore()
	const modalStore = getModalStore()

	const emailPattern = new RegExp('[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,}$')

	const verify = async () => {

		if (!user?.email || !user?.oid) {
			toastStore.trigger(errorToast("No user information found."))
			return
		}

		const sendRecover = await fetch(`/auth/sendActivationCode`, {
			method: 'POST',
			body: JSON.stringify({ email: user.email, userId: user.oid })
		})

		if (!sendRecover.ok) {

			toastStore.trigger(errorToast("Email could not be sent."))
			return
		} else {

			const codeResponse = await showCodeModal(
				{
					title: "Verification",
					email: user.email,
					userId: user.oid,
					message: "Please enter the activation code sent to your email",
					endpoint: "verifyActivationCode"
				},
				modalStore
			)

			if (codeResponse) {

				userSession.update((u) => {
					return { ...u, activated: true }
				})


				if ($userSession.activated) toastStore.trigger(successToast("Email verified successfully."))

				await goto('/account')

			} else {
				toastStore.trigger(errorToast("Verification failed."))
			}
		}


	}

	const logout = async () => {
		const response = await fetch('/account/logout', { method: 'PUT' })
		if (response.ok) {
			userSession.set(undefined)

			await goto('/account/login')
		} else {
			console.error(response.status, response.statusText)
		}
	}

</script>

<svelte:head>
	<title>Verification</title>
</svelte:head>

<div class="container max-w-4xl mx-auto h-full flex items-center">
	<div>
		<div class="section-wrapper p-8 space-y-16  scroll-mt-4" id="top">
			<div class="space-y-8 text-center">
				<h2 class="h2">Your account is not yet verified. </h2>
				<p class="max-w-[60ch] mx-auto">
					Your account is not yet verified.
					Either verify your email address or logout. After verification, you will be able
					to access this website unrestricted.
				</p>
			</div>
			<div class="space-y-4 ">
					<p class="max-w-[60ch]  p-4 rounded-md mx-auto">
						To verify your email, please click the "Verify Email" button below.
						An email will be sent to your registered email address with a verification code which
						you will need to enter to complete the verification process.
					</p>

			</div>
			<div class="flex flex-row justify-between gap-4 ">

				<button
					class="button-primary button-modal self-center dark:!variant-filled-primary"
					on:click={verify}
				>
					<Fa icon={faEnvelope} size="lg" />
					<span>Verify Email</span>
				</button>

				<button
					class="button-error button-modal self-center dark:!variant-filled-primary"
					on:click={logout}
				>
					<Fa icon={faArrowRightFromBracket} size="lg" />
					<span>Logout</span>
				</button>
			</div>
		</div>
	</div>
</div>
