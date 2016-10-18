package cnn.core.structure

import cnn.activation.functions.Softmax
import cnn.activation.functions._
import cnn.exceptions.InvalidActivationFuncException
import scala.annotation.tailrec
import cnn.exceptions.NeuralLinkException
import cnn.exceptions.MatCountException
import cnn.exceptions.FCLayerStructureException
import cnn.exceptions.NeuronActivationException
import cnn.exceptions.{FC_LINK_COUNT, NEURON_INVALID_ACTIVATION}



class Neuron(inLinks : Vector[Link], val outLinks : Vector[Link],  activationFun : ActivationFunction, act : Double = 0, preact : Double =0 ,  derivative : Double = 0) 
  extends NeuralUnit
{
  
  val _inLinks = inLinks
  val _activationFun = activationFun
  val _act = act
  val _der = derivative
  val _preact = preact
  
  def this(inLinks : Vector[Link], activationFun : ActivationFunction) =
    this(inLinks, Vector(), activationFun, 0,0,0)
  
  
  def apply() = inLinks match{
      case h +: t => val act = computeActivation(computePreactivation())
                           updateWithActivation(act)
      case _      => throw new NeuralLinkException(FC_LINK_COUNT)
  }
  
  private def computeActivation(preactivation : Double) : Double = this match {
    case n @ _ => n._activationFun match {
      case `_SOFTMAX` =>  throw new InvalidActivationFuncException(NEURON_INVALID_ACTIVATION)
      case `_SIGMOID` => Sigmoid(n._act) 
    }
  }
  
  def computePreactivation() = inLinks.foldLeft(0.0)((x,y)=> x + y.*)
  
  def computeDelta() = _activationFun match {
      case `_SOFTMAX` =>  throw new InvalidActivationFuncException(NEURON_INVALID_ACTIVATION)
      case `_SIGMOID` => Sigmoid.derivative(_act)
  }

  
  def updateWithDerivative(d : Double) = new Neuron(inLinks, outLinks, activationFun, act, d)
  def updateWithActivation(a : Double) : Neuron = new Neuron(inLinks, outLinks, activationFun, a, derivative)
  def updateWithInput(in : Vector[Link]) = new Neuron(in, outLinks, activationFun, act, derivative)
}


/**Output layer neuron**/

case class OutNeuron(inLinks : Vector[Link], activationFun : ActivationFunction, classification : Int, act : Double , preact : Double , derivative : Double) 
  extends Neuron(inLinks, Vector(), activationFun, act, preact, derivative){
  
  def this(_inLinks : Vector[Link], _classification: Int) = this(_inLinks, _SOFTMAX, _classification,0,0,0)
  
  def apply(layerPreact : Layer[OutNeuron]) = inLinks match {
    case a@ h+:t => val act = computeActivation(layerPreact.get.map(_.preact))
                    updateWithActivation(act)
    case _       => throw new NeuralLinkException(FC_LINK_COUNT)
  }

  private def computeActivation( layerPreact : Seq[Double]) : Double = this match {
    case o @ OutNeuron(in, func, cla, act, preact, der) =>  o.activationFun match{
      case `_SOFTMAX` => Softmax.apply(layerPreact, preact)
      case `_SIGMOID` => Sigmoid(act)
    }
  }
 
  def computeDelta(target : Int) = activationFun match{
      case `_SOFTMAX` => Softmax.derivative(this, target)
      case `_SIGMOID` => Sigmoid.derivative(act)
  }
  
  override def updateWithActivation(a : Double) = OutNeuron(inLinks, activationFun, classification, a, preact, derivative)
  override def updateWithDerivative(d: Double) = OutNeuron(inLinks, activationFun ,classification, act, preact, d)
  override def updateWithInput(in : Vector[Link]) =  OutNeuron(in, activationFun, classification ,act, preact, derivative)
  def updateWithPreact(p : Double) = OutNeuron(inLinks, activationFun, classification, act, p, derivative)
  
}

/**Companion**/
object Neuron{
  def unapply(n : Neuron) : Option[ActivationFunction] = Some(n._activationFun)
}