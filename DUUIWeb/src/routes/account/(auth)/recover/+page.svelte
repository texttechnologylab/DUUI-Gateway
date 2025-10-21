<script lang="ts">
	import Text from '$lib/svelte/components/Input/TextInput.svelte'
	import { faCheck, faEnvelope, faEnvelopeCircleCheck } from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'
	import { fly } from 'svelte/transition'
	import { getModalStore, getToastStore } from '@skeletonlabs/skeleton'
	import { showCodeModal } from '$lib/svelte/utils/modal'
	import Password from '$lib/svelte/components/Input/Password.svelte'
	import { errorToast, successToast } from '$lib/duui/utils/ui'
	import Tip from '$lib/svelte/components/Tip.svelte'
	import { userSession } from '$lib/store'
	import { goto } from '$app/navigation'
	import { env } from '$env/dynamic/public';


	const requireVerification =
		(env.PUBLIC_VERIFICATION_REQUIRED ?? 'false').toLowerCase() !== 'false';

	let recoverAddress: string = ''
	let message: string = ''

	let userId: string

	let passwordError: boolean
	let password1: string
	let password2: string

	let sent: boolean = false
	let verified: boolean = false

	const toastStore = getToastStore()
	const modalStore = getModalStore()

	const emailPattern = new RegExp('[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,}$')

	const verify = async () => {
		if (!emailPattern.test(recoverAddress)) {
			message = 'Please enter a valid E-Mail address'
			return
		}
		const verifyEmail = await fetch(`/auth/verifyEmail`, {
			method: 'POST',
			body: JSON.stringify({ email: recoverAddress })
		})

		const emailMsg = await verifyEmail.json()

		if (!verifyEmail.ok) {

			message = emailMsg.message
			return
		} else if (!requireVerification) {
				userId = emailMsg.user

				message = ""
				sent = true
				verified = true
				return
		} else {

			userId = emailMsg.user

			const sendRecover = await fetch(`/auth/sendRecoveryCode`, {
				method: 'POST',
				body: JSON.stringify({ email: recoverAddress, userId: userId })
			})

			if (!sendRecover.ok) {
				const recoveryMessage = (await sendRecover.json()).error

				toastStore.trigger(errorToast("Email could not be sent."))
				return
			} else {

				const codeResponse = await showCodeModal(
					{
						title: "Recovery",
						email: recoverAddress,
						userId: userId,
						message: "Please enter the recovery code sent to your email",
						endpoint: "verifyRecoveryCode"
					},
					modalStore
				)

				verified = !!codeResponse
			}
		}
	}

	const recover = async () => {

		const request = {
			method: 'POST',
			body: JSON.stringify({
				oid: userId,
				password1: password1,
				password2: password2
			}),
			headers: {
				'Content-Type': 'application/json'
			}
		}

		const response = await fetch('/auth/change', request)

		if (response.ok) {
			toastStore.trigger(successToast('Update successful'))
			passwordError = false

			const request = {
				method: 'POST',
				body: JSON.stringify({
					email: recoverAddress,
					password: password2
				}),
				headers: {
					'Content-Type': 'application/json'
				}
			}

			const response = await fetch('/auth/login', request)

			const result = await response.json()

			if (response.ok) {
				userSession.set(result.user)

				await goto("/account")
			} else {
				if (response.status === 503) message = 'Could not reach the server. Try again later.'
				else message = result
			}
		} else {
			toastStore.trigger(errorToast(await response.json()))
			passwordError = response.status === 422;

		}
	}
</script>

<svelte:head>
	<title>Recover</title>
</svelte:head>

<div class="container max-w-4xl mx-auto h-full flex items-center">
	<div>
		{#if sent}
			<div class="section-wrapper p-8 space-y-2 max-w-[60ch]">
				<p>
					An email has been sent to <span class="font-bold">{recoverAddress}</span>.
				</p>
				<p>Check your inbox to reset your password.</p>
				<Fa icon={faEnvelopeCircleCheck} size="lg" />
			</div>
		{:else}
			<div class="section-wrapper p-8 space-y-16 scroll-mt-4" id="top">
				<div class="space-y-8">
					<h2 class="h2">Recover Password</h2>
					<p class="max-w-[60ch]">
						Enter the E-Mail of your account below and we will send you a link to update your
						password.
					</p>
				</div>
				<div class="space-y-4 relative">
					{#if message}
						<p in:fly={{ y: 10 }} class=" font-bold variant-soft-error p-4 rounded-md max-w-[40ch]">
							{message}
						</p>
					{/if}
					<Text label="Email" name="email" bind:value={recoverAddress} />

					<button
						class="button-primary button-modal self-center dark:!variant-filled-primary"
						on:click={verify}
					>
						<Fa icon={faEnvelope} size="lg" />
						<span>Verify Email</span>
					</button>

					{#key verified}
						<Tip tipTheme={verified ? "success" : "error" }>
							{#if verified}
								<p class="font-bold">Email verified! You can now reset your password.</p>
							{:else}
								<p class="font-bold">Please verify your email to reset your password.</p>
							{/if}
						</Tip>
						<Password bind:value={password1} label="Password" name="password" required disabled={!verified} />
						<Password
							bind:value={password2}
							label="Repeat Password"
							name="password2"
							required

							disabled={!verified}
							style={passwordError ? "text-red-500 border-4 border-red-400 rounded-lg px-2 py-1" : ""}
							on:keydown={async (event) =>{
								if (event.key === 'Enter') {
									await recover()
								}
							}}
						/>
					{/key}
					<button
						class="button-primary button-modal self-center dark:!variant-filled-primary"
						on:click={recover}
					>
						<Fa icon={faCheck} size="lg" />
						<span>Reset Password</span>
					</button>
				</div>
			</div>
		{/if}
	</div>
</div>
