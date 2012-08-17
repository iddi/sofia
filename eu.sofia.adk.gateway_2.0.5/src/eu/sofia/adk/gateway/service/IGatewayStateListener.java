package eu.sofia.adk.gateway.service;

public interface IGatewayStateListener {
	public void gatewayStopped(IGateway gw);
	public void gatewayRunning(IGateway gw);
}
