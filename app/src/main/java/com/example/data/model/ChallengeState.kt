package com.example.data.model

sealed class ChallengeState {
    object Idle : ChallengeState()
    object AwaitingPin : ChallengeState()
    object AwaitingOtp : ChallengeState()
    object AwaitingRemoteApproval : ChallengeState()
    object Granted : ChallengeState()
    object Rejected : ChallengeState()
}
